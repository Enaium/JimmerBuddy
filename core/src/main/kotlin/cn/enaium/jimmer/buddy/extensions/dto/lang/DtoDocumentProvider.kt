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

import cn.enaium.jimmer.buddy.extensions.dto.completion.getTrace
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiAliasGroup
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiAliasPattern
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiMacro
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiRoot
import cn.enaium.jimmer.buddy.utility.*
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.util.findParentOfType
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.KtClass

/**
 * @author Enaium
 */
class DtoDocumentProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? {
        element.findParentOfType<DtoPsiMacro>()?.also { element ->
            val name = element.name?.value ?: return null
            val project = element.project
            val trace = getTrace(element)
            val typeName = element.findParentOfType<DtoPsiRoot>()?.qualifiedName() ?: return null
            val commonImmutable = if (project.isJavaProject()) {
                JavaPsiFacade.getInstance(project).findClass(typeName, project.allScope())?.toImmutable()
                    ?.toCommonImmutableType() ?: return null
            } else if (project.isKotlinProject()) {
                (KotlinFullClassNameIndex[typeName, project, project.allScope()].firstOrNull() as? KtClass)?.toImmutable()
                    ?.toCommonImmutableType() ?: return null
            } else {
                return null
            }

            var currentImmutable = commonImmutable

            trace.forEach { trace ->
                currentImmutable.props().find { it.name() == trace }?.targetType()?.also {
                    currentImmutable = it
                }
            }

            val props = when (name) {
                "allScalars" -> {
                    currentImmutable.props().filter { isAutoScalar(it) }
                }

                "allReferences" -> {
                    currentImmutable.props().filter { isAutoReference(it) }
                }

                else -> {
                    emptyList()
                }
            }

            val content = """
                ## $name
                
                ${props.joinToString(", ") { "`${it.name()}`" }}
            """.trimIndent()
            return content.toHtml()
        }

        element.findParentOfType<DtoPsiAliasGroup>()?.also { group ->
            val positiveProps = group.body?.positiveProps?.takeIf { it.isNotEmpty() } ?: return null
            val parent = element.parent
            if (parent is DtoPsiAliasPattern) {
                var content = "## Alias Group"
                positiveProps.forEach {
                    content += "\n\n`${it.prop?.value}` as ${pattern(it.prop?.value ?: "", parent)}"
                }
                return content.toHtml()
            }
        }

        return null
    }

    private fun pattern(name: String, pattern: DtoPsiAliasPattern): String {
        val original = pattern.original
        val replace = pattern.replacement?.value ?: ""
        return if (pattern.prefix) {
            if (original == null) {
                "`${replace}${name.replaceFirstChar { it.uppercase() }}`"
            } else {
                "`${
                    name.replaceFirst(
                        original.text,
                        replace
                    )
                }`"
            }
        } else if (pattern.suffix) {
            if (original == null) {
                "`${name}${replace}`"
            } else {
                "`${
                    name.replaceFirst(
                        original.text,
                        replace
                    )
                }`"
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

    private fun isAutoScalar(baseProp: CommonImmutableType.CommonImmutableProp): Boolean {
        return !baseProp.isFormula() &&
                !baseProp.isTransient() &&
                !baseProp.isIdView() &&
                !baseProp.isManyToManyView() &&
                !baseProp.isList() &&
                !baseProp.isAssociation(true) &&
                !baseProp.isLogicalDeleted() &&
                !baseProp.isExcludedFromAllScalars()
    }
}