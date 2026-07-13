/*
 * Copyright 2025 Enaium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.enaium.jimmer.buddy.extensions.debugger

import com.intellij.debugger.DebuggerContext
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.FullValueEvaluatorProvider
import com.intellij.debugger.engine.evaluation.EvaluationContext
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.impl.DebuggerUtilsAsync
import com.intellij.debugger.ui.impl.watch.ValueDescriptorImpl
import com.intellij.debugger.ui.tree.DebuggerTreeNode
import com.intellij.debugger.ui.tree.NodeDescriptor
import com.intellij.debugger.ui.tree.ValueDescriptor
import com.intellij.debugger.ui.tree.render.ChildrenBuilder
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener
import com.intellij.debugger.ui.tree.render.NodeRendererImpl
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiElement
import com.intellij.xdebugger.frame.XFullValueEvaluator
import com.sun.jdi.ArrayReference
import com.sun.jdi.BooleanValue
import com.sun.jdi.ObjectReference
import com.sun.jdi.PrimitiveValue
import com.sun.jdi.ReferenceType
import com.sun.jdi.StringReference
import com.sun.jdi.Value
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.CompletableFuture

/**
 * Renders Jimmer immutable objects by their domain properties instead of generated `__*` backing fields.
 *
 * The Jimmer runtime exposes loaded state through `ImmutableSpi`, while generated implementations keep
 * `__<prop>Loaded` and `__<prop>Value` fields. Reading those fields lets the debugger show both loaded
 * and unloaded properties without calling property getters that can throw unloaded-property exceptions.
 *
 * @author Enaium
 */
class JimmerImmutableRenderer : NodeRendererImpl("Jimmer Immutable", true), FullValueEvaluatorProvider {

    init {
        setIsApplicableChecker { type ->
            if (type is ReferenceType) {
                DebuggerUtilsAsync.instanceOf(type, IMMUTABLE_SPI)
            } else {
                CompletableFuture.completedFuture(false)
            }
        }
    }

    override fun getUniqueId(): String {
        return "JimmerImmutableRenderer"
    }

    override fun calcLabel(
        descriptor: ValueDescriptor,
        evaluationContext: EvaluationContext,
        listener: DescriptorLabelListener
    ): String {
        val value = descriptor.value as? ObjectReference ?: return "Jimmer immutable"
        val props = value.jimmerProps()
        if (props.isEmpty()) {
            return DebugProcessImpl.getDefaultRenderer(value).calcLabel(descriptor, evaluationContext, listener)
        }

        val loaded = props.count { it.loaded }
        val unloaded = props.size - loaded
        return "loaded $loaded/${props.size}, unloaded $unloaded | ${value.compactShape(props)}"
    }

    override fun buildChildren(value: Value, builder: ChildrenBuilder, evaluationContext: EvaluationContext) {
        val reference = value as? ObjectReference ?: return
        val props = reference.jimmerProps()
        if (props.isEmpty()) {
            DebugProcessImpl.getDefaultRenderer(value).buildChildren(value, builder, evaluationContext)
            return
        }

        val nodes = mutableListOf<DebuggerTreeNode>()
        val project = evaluationContext.project
        val loadedProps = props.filter { it.loaded }
        val unloadedProps = props.filterNot { it.loaded }

        nodes += builder.nodeManager.createMessageNode(
            "Loaded: ${loadedProps.namesOrEmpty()} | Unloaded: ${unloadedProps.namesOrEmpty()}"
        )
        props.forEach { prop ->
            if (prop.loaded) {
                nodes += builder.nodeManager.createNode(
                    JimmerPropertyDescriptor(project, "✓ ${prop.name}", prop.name, prop.value),
                    evaluationContext
                )
            } else {
                nodes += builder.nodeManager.createMessageNode("✗ ${prop.name} = <unloaded>")
            }
        }

        builder.setChildren(nodes)
    }

    override fun isExpandableAsync(
        value: Value,
        evaluationContext: EvaluationContext,
        parentDescriptor: NodeDescriptor
    ): CompletableFuture<Boolean> {
        if (value !is ObjectReference) {
            return CompletableFuture.completedFuture(false)
        }
        val props = value.jimmerProps()
        return if (props.isEmpty()) {
            DebugProcessImpl.getDefaultRenderer(value).isExpandableAsync(value, evaluationContext, parentDescriptor)
        } else {
            CompletableFuture.completedFuture(true)
        }
    }

    override fun getChildValueExpression(node: DebuggerTreeNode, context: DebuggerContext): PsiElement? {
        return (node.descriptor as? JimmerPropertyDescriptor)?.getDescriptorEvaluation(context)
    }

    override fun getFullValueEvaluator(
        evaluationContext: EvaluationContextImpl,
        descriptor: ValueDescriptorImpl
    ): XFullValueEvaluator? {
        val value = descriptor.value as? ObjectReference ?: return null
        if (value.jimmerProps().isEmpty()) {
            return null
        }
        val json = value.debugJson().prettyJson()
        return object : XFullValueEvaluator("Jimmer JSON") {
            override fun startEvaluation(callback: XFullValueEvaluationCallback) {
                callback.evaluated(json)
            }
        }
    }

    private data class JimmerProp(
        val name: String,
        val loaded: Boolean,
        val value: Value?
    )

    private class JimmerPropertyDescriptor(
        project: Project,
        private val displayName: String,
        private val propName: String,
        private val propValue: Value?
    ) : ValueDescriptorImpl(project, propValue) {

        override fun calcValue(evaluationContext: EvaluationContextImpl): Value? {
            return propValue
        }

        override fun calcValueName(): String {
            return displayName
        }

        override fun getDescriptorEvaluation(context: DebuggerContext): PsiExpression {
            return JavaPsiFacade.getElementFactory(context.project)
                .createExpressionFromText("__get(\"${propName.escapeJavaString()}\")", null)
        }
    }

    private companion object {
        private const val IMMUTABLE_SPI = "org.babyfish.jimmer.runtime.ImmutableSpi"
        private const val FIELD_PREFIX = "__"
        private const val LOADED_SUFFIX = "Loaded"
        private const val VALUE_SUFFIX = "Value"
        private const val LABEL_VALUE_LIMIT = 6
        private const val JSON_DEPTH_LIMIT = 5
        private const val JSON_ARRAY_LIMIT = 50

        private fun ObjectReference.jimmerProps(): List<JimmerProp> {
            return referenceType()
                .allFields()
                .asSequence()
                .filter { field ->
                    field.name().startsWith(FIELD_PREFIX) &&
                        field.name().endsWith(LOADED_SUFFIX) &&
                        field.typeName() == "boolean"
                }
                .map { loadedField ->
                    val fieldName = loadedField.name()
                    val propName = fieldName.substring(FIELD_PREFIX.length, fieldName.length - LOADED_SUFFIX.length)
                    val valueField = referenceType().fieldByName("$FIELD_PREFIX$propName$VALUE_SUFFIX")
                    JimmerProp(
                        name = propName,
                        loaded = (getValue(loadedField) as? BooleanValue)?.value() == true,
                        value = valueField?.let { getValue(it) }
                    )
                }
                .toList()
        }

        private fun ObjectReference.compactShape(props: List<JimmerProp>): String {
            val typeName = referenceType().name().substringAfterLast('.').substringBefore("$")
            return props
                .asSequence()
                .take(LABEL_VALUE_LIMIT)
                .joinToString(", ") { prop ->
                    if (prop.loaded) {
                        "${prop.name}=${prop.value.debugText()}"
                    } else {
                        "${prop.name}=<unloaded>"
                    }
                }
                .let { "$typeName{$it${if (props.size > LABEL_VALUE_LIMIT) ", ..." else ""}}" }
        }

        private fun Collection<JimmerProp>.namesOrEmpty(): String {
            return if (isEmpty()) {
                "-"
            } else {
                joinToString(", ") { it.name }
            }
        }

        private fun Value?.debugText(): String {
            return when (this) {
                null -> "null"
                is StringReference -> "\"${value().abbreviate(80).escapeControlChars()}\""
                else -> toString().abbreviate(80)
            }
        }

        private fun ObjectReference.debugJson(): String {
            val visited = Collections.newSetFromMap(IdentityHashMap<ObjectReference, Boolean>())
            return toJson(0, visited)
        }

        private fun Value?.toJson(depth: Int, visited: MutableSet<ObjectReference>): String {
            return when (this) {
                null -> "null"
                is PrimitiveValue -> toString()
                is StringReference -> value().jsonString()
                is ArrayReference -> toJsonArray(depth, visited)
                is ObjectReference -> toJsonObject(depth, visited)
                else -> toString().jsonString()
            }
        }

        private fun ArrayReference.toJsonArray(depth: Int, visited: MutableSet<ObjectReference>): String {
            if (depth >= JSON_DEPTH_LIMIT) {
                return "\"<max-depth>\""
            }
            val values = values.take(JSON_ARRAY_LIMIT)
            val suffix = if (length() > JSON_ARRAY_LIMIT) listOf("\"... ${length() - JSON_ARRAY_LIMIT} more\"") else emptyList()
            return (values.map { it.toJson(depth + 1, visited) } + suffix)
                .joinToString(prefix = "[", postfix = "]")
        }

        private fun ObjectReference.toJsonObject(depth: Int, visited: MutableSet<ObjectReference>): String {
            if (depth >= JSON_DEPTH_LIMIT) {
                return "\"<max-depth>\""
            }
            if (!visited.add(this)) {
                return "\"<cycle:${uniqueID()}>\""
            }

            try {
                val jimmerProps = jimmerProps()
                if (jimmerProps.isNotEmpty()) {
                    return jimmerProps.toJimmerJson(depth, visited)
                }

                return when {
                    isEnumLike() -> enumName().jsonString()
                    isArrayListLike() -> arrayListValues().toJsonArrayLike(depth, visited)
                    referenceType().name().startsWith("java.") -> toString().jsonString()
                    else -> objectFieldsJson(depth, visited)
                }
            } finally {
                visited.remove(this)
            }
        }

        private fun List<JimmerProp>.toJimmerJson(depth: Int, visited: MutableSet<ObjectReference>): String {
            val entries = mutableListOf<String>()
            filter { it.loaded }.forEach { prop ->
                entries += "${prop.name.jsonString()}: ${prop.value.toJson(depth + 1, visited)}"
            }
            val unloaded = filterNot { it.loaded }
            if (unloaded.isNotEmpty()) {
                entries += "${"_unloaded".jsonString()}: ${unloaded.map { it.name.jsonString() }.joinToString(prefix = "[", postfix = "]")}" 
            }
            return entries.joinToString(prefix = "{", postfix = "}")
        }

        private fun ObjectReference.objectFieldsJson(depth: Int, visited: MutableSet<ObjectReference>): String {
            val entries = referenceType()
                .allFields()
                .asSequence()
                .filterNot { it.isStatic() || it.name().startsWith("this$") }
                .map { field -> "${field.name().jsonString()}: ${getValue(field).toJson(depth + 1, visited)}" }
                .toList()
            return entries.joinToString(prefix = "{", postfix = "}")
        }

        private fun ObjectReference.isEnumLike(): Boolean {
            return referenceType().allFields().any { it.name() == "name" && it.declaringType().name() == "java.lang.Enum" }
        }

        private fun ObjectReference.enumName(): String {
            val nameField = referenceType().allFields().firstOrNull { it.name() == "name" && it.declaringType().name() == "java.lang.Enum" }
            return ((nameField?.let { getValue(it) }) as? StringReference)?.value() ?: toString()
        }

        private fun ObjectReference.isArrayListLike(): Boolean {
            val name = referenceType().name()
            return name == "java.util.ArrayList" || name == "java.util.Arrays${'$'}ArrayList"
        }

        private fun ObjectReference.arrayListValues(): List<Value?> {
            val valuesField = referenceType().fieldByName("elementData") ?: referenceType().fieldByName("a") ?: return emptyList()
            val array = getValue(valuesField) as? ArrayReference ?: return emptyList()
            val size = (referenceType().fieldByName("size")?.let { getValue(it) } as? PrimitiveValue)?.toString()?.toIntOrNull()
                ?: array.length()
            return array.values.take(size.coerceAtMost(array.length()))
        }

        private fun List<Value?>.toJsonArrayLike(depth: Int, visited: MutableSet<ObjectReference>): String {
            val values = take(JSON_ARRAY_LIMIT)
            val suffix = if (size > JSON_ARRAY_LIMIT) listOf("\"... ${size - JSON_ARRAY_LIMIT} more\"") else emptyList()
            return (values.map { it.toJson(depth + 1, visited) } + suffix)
                .joinToString(prefix = "[", postfix = "]")
        }

        private fun String.prettyJson(): String {
            val builder = StringBuilder(length * 2)
            var indent = 0
            var inString = false
            var escaping = false
            forEach { char ->
                if (inString) {
                    builder.append(char)
                    if (escaping) {
                        escaping = false
                    } else if (char == '\\') {
                        escaping = true
                    } else if (char == '"') {
                        inString = false
                    }
                    return@forEach
                }

                when (char) {
                    '"' -> {
                        inString = true
                        builder.append(char)
                    }
                    '{', '[' -> {
                        builder.append(char)
                        indent++
                        builder.append('\n')
                        builder.append("  ".repeat(indent))
                    }
                    '}', ']' -> {
                        indent--
                        builder.append('\n')
                        builder.append("  ".repeat(indent))
                        builder.append(char)
                    }
                    ',' -> {
                        builder.append(char)
                        builder.append('\n')
                        builder.append("  ".repeat(indent))
                    }
                    ':' -> builder.append(": ")
                    else -> builder.append(char)
                }
            }
            return builder.toString()
        }

        private fun String.abbreviate(maxLength: Int): String {
            return if (length <= maxLength) {
                this
            } else {
                "${take(maxLength - 3)}..."
            }
        }

        private fun String.escapeControlChars(): String {
            return buildString(length) {
                this@escapeControlChars.forEach { char ->
                    when (char) {
                        '\n' -> append("\\n")
                        '\r' -> append("\\r")
                        '\t' -> append("\\t")
                        else -> append(char)
                    }
                }
            }
        }

        private fun String.escapeJavaString(): String {
            return buildString(length) {
                this@escapeJavaString.forEach { char ->
                    when (char) {
                        '\\' -> append("\\\\")
                        '"' -> append("\\\"")
                        '\n' -> append("\\n")
                        '\r' -> append("\\r")
                        '\t' -> append("\\t")
                        else -> append(char)
                    }
                }
            }
        }

        private fun String.jsonString(): String {
            return buildString(length + 2) {
                append('"')
                this@jsonString.forEach { char ->
                    when (char) {
                        '\\' -> append("\\\\")
                        '"' -> append("\\\"")
                        '\n' -> append("\\n")
                        '\r' -> append("\\r")
                        '\t' -> append("\\t")
                        else -> append(char)
                    }
                }
                append('"')
            }
        }
    }
}
