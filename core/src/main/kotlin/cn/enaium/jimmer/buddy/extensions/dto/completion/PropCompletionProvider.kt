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

package cn.enaium.jimmer.buddy.extensions.dto.completion

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiFunc
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiFuncArguments
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiPositiveProp
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiPropName
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiTypeBranch
import cn.enaium.jimmer.buddy.utility.CommonImmutableProp.Companion.type
import cn.enaium.jimmer.buddy.utility.PROP
import cn.enaium.jimmer.buddy.utility.findCurrentImmutableType
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.util.findParentOfType
import com.intellij.util.ProcessingContext

/**
 * @author Enaium
 */
object PropCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val element = parameters.position
        val propName = element.findParentOfType<DtoPsiPropName>()
        val funcArguments = (propName?.parent ?: element.parent) as? DtoPsiFuncArguments
        if (funcArguments != null) {
            // Completing a func argument, e.g. the prop inside flat(store): only
            // association/embedded props can be flattened, and no body template applies
            val isFlat = (funcArguments.parent as? DtoPsiFunc)?.funcTarget?.text == "flat"
            findCurrentImmutableType(element)?.props
                ?.filter { !isFlat || it.isAssociation || it.isEmbedded }
                ?.forEach { prop ->
                    result.addElement(
                        LookupElementBuilder.create(prop.name).withIcon(JimmerBuddy.Icons.PROP)
                            .withTypeText(prop.type().description)
                    )
                }
            return
        }
        findCurrentImmutableType(element)?.props?.forEach { prop ->
            result.addElement(
                LookupElementBuilder.create(prop.name).withIcon(JimmerBuddy.Icons.PROP)
                    .withTailText(" (from ${prop.name})").withTypeText(prop.type().description).let {
                        if (prop.isRecursive) {
                            it.withInsertHandler { context, _ ->
                                val project = context.project
                                val editor = context.editor

                                WriteCommandAction.runWriteCommandAction(project) {
                                    val tm = TemplateManager.getInstance(project)
                                    val template: Template = tm.createTemplate("", "")
                                    template.isToReformat = true
                                    template.addTextSegment("*")
                                    template.addEndVariable()
                                    tm.startTemplate(editor, template)
                                }
                            }
                        } else if (prop.isAssociation) {
                            it.withInsertHandler { context, _ ->
                                val project = context.project
                                val editor = context.editor

                                WriteCommandAction.runWriteCommandAction(project) {
                                    val tm = TemplateManager.getInstance(project)
                                    val template: Template = tm.createTemplate("", "")
                                    template.isToReformat = true
                                    template.addTextSegment(" {\n")
                                    template.addTextSegment("\t")
                                    template.addEndVariable()
                                    template.addTextSegment("\n}")
                                    tm.startTemplate(editor, template)
                                }
                            }
                        } else {
                            it
                        }
                    }
            )
        }
    }
}

fun getTrace(position: PsiElement?): List<String> {
    val trace = mutableListOf<String>()
    var parent: PsiElement? = position?.parent
    // A func argument (e.g. the prop inside flat(store)) resolves against the
    // parent type of its host prop, so the host prop contributes no segment
    var skipHostProp = position is DtoPsiFuncArguments ||
            (position as? DtoPsiPropName)?.parent is DtoPsiFuncArguments
    while (parent != null) {
        if (parent is DtoPsiPositiveProp) {
            if (skipHostProp) {
                skipHostProp = false
            } else {
                val name = parent.propName?.identifier?.text
                    // flat(store) { ... }: the body resolves against the target type
                    // of the flat argument, so the argument is the descent segment
                    ?: parent.func?.takeIf { it.funcTarget.text == "flat" }
                        ?.funcArguments?.propNameList?.firstOrNull()?.identifier?.text
                name?.also { trace.add(it) }
            }
        } else if (parent is DtoPsiTypeBranch) {
            parent.qualifiedName.text.split(".").lastOrNull()?.also { trace.add(it) }
        }
        parent = parent.parent
    }
    return trace.reversed()
}