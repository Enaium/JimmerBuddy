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

package cn.enaium.jimmer.buddy.extensions.dto.lang

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiAliasGroup
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiAliasPattern
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiMacro
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoTypes
import cn.enaium.jimmer.buddy.utility.CommonImmutableType
import cn.enaium.jimmer.buddy.utility.CommonImmutableType.CommonImmutableProp.Companion.isAutoScalar
import cn.enaium.jimmer.buddy.utility.findCurrentImmutableType
import cn.enaium.jimmer.buddy.utility.toHtml
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.findParentOfType

/**
 * @author Enaium
 */
class DtoDocumentProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? {
        element.findParentOfType<DtoPsiMacro>()?.also { macro ->
            val name = macro.firstChild.text ?: return null
            val currentImmutable = findCurrentImmutableType(macro) ?: return null

            val props = when (name) {
                "allScalars" -> {
                    val commonImmutableProps = mutableListOf<CommonImmutableType.CommonImmutableProp>()
                    if (macro.qualifiedNameList.isEmpty()) {
                        commonImmutableProps.addAll(currentImmutable.props())
                    } else {
                        macro.qualifiedNameList.forEach { arg ->
                            when (arg.text) {
                                "this", currentImmutable.name() -> commonImmutableProps.addAll(currentImmutable.declaredProps())
                                else -> commonImmutableProps.addAll(
                                    currentImmutable.superTypes().find { it.qualifiedName() == it.qualifiedName() }
                                        ?.declaredProps() ?: emptyList()
                                )
                            }
                        }
                    }
                    commonImmutableProps.filter { it.isAutoScalar() }
                }

                "allReferences" -> {
                    currentImmutable.props().filter { isAutoReference(it) }
                }

                else -> {
                    emptyList()
                }
            }

            val content = buildString {
                append("## $name")

                val aliasGroup = element.findParentOfType<DtoPsiAliasGroup>()
                aliasGroup?.aliasPattern?.also { aliasPattern ->
                    props.forEach { prop ->
                        append("\n\n`${prop.name()}` as ${pattern(prop.name(), aliasPattern)}")
                    }
                    return@buildString
                }

                append("\n\n${props.joinToString(", ") { "`${it.name()}`" }}")
            }
            return content.toHtml()
        }

        element.findParentOfType<DtoPsiAliasGroup>()?.also { group ->
            val positiveProps = group.positivePropList.takeIf { it.isNotEmpty() } ?: return null
            val parent = element.parent
            if (parent is DtoPsiAliasPattern) {
                var content = "## Alias Group"
                positiveProps.forEach { positiveProp ->
                    val propName = positiveProp.children.firstOrNull { it.node.elementType == DtoTypes.IDENTIFIER }?.text ?: ""
                    content += "\n\n`${propName}` as ${pattern(propName, parent)}"
                }
                return content.toHtml()
            }
        }

        return null
    }

    private fun pattern(name: String, pattern: DtoPsiAliasPattern): String {
        val children = pattern.children.toList()
        val hasCaret = children.any { it.node.elementType == DtoTypes.CARET }
        val hasDollar = children.any { it.node.elementType == DtoTypes.DOLLAR }
        val identifiers = children.filter { it.node.elementType == DtoTypes.IDENTIFIER }
        val original = if (hasCaret && identifiers.isNotEmpty()) identifiers.first() else null
        val replace = if (hasDollar) identifiers.lastOrNull()?.text ?: "" else identifiers.lastOrNull()?.text ?: ""
        return if (hasCaret) {
            if (original == null) {
                "`${replace}${name.replaceFirstChar { it.uppercase() }}`"
            } else {
                "`${name.replaceFirst(original.text, replace)}`"
            }
        } else if (hasDollar) {
            if (original == null) {
                "`${name}${replace}`"
            } else {
                "`${name.replaceFirst(original.text, replace)}`"
            }
        } else {
            if (original != null) {
                "`${name.replaceFirst(original.text, replace)}`"
            } else {
                ""
            }
        }
    }

    private fun isAutoReference(baseProp: CommonImmutableType.CommonImmutableProp): Boolean {
        return baseProp.isAssociation(true) && !baseProp.isList() && !baseProp.isTransient()
    }
}