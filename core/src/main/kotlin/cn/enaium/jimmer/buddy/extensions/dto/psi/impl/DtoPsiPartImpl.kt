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

package cn.enaium.jimmer.buddy.extensions.dto.psi.impl

import cn.enaium.jimmer.buddy.extensions.dto.psi.*
import cn.enaium.jimmer.buddy.utility.findCurrentImmutableType
import com.intellij.lang.ASTNode
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.util.findParentOfType
import org.jetbrains.kotlin.idea.base.util.allScope

class DtoPsiPartImpl(node: ASTNode) : DtoPsiNamedElement(node), DtoPsiPart {
    override fun getName(): String = text
    override fun reference(): PsiElement? {
        val parentElement = parent
        val facade = JavaPsiFacade.getInstance(project)
        if (parentElement is DtoPsiTypeParts) {
            parentElement.qualifiedNameParts?.also { qualifiedNameParts ->
                return if (qualifiedNameParts.parts.last() == this) {
                    facade.findClass(parentElement.qualifiedName() ?: return null, project.allScope())
                } else {
                    facade
                        .findPackage(
                            qualifiedNameParts.parts.subList(0, qualifiedNameParts.parts.indexOf(this) + 1)
                                .joinToString(".") { it.text })
                }
            }
        }

        if (parentElement is DtoPsiQualifiedNameParts) {
            return if (parentElement.parts.last() == this) {
                if (parentElement.findParentOfType<DtoPsiMacro>() != null) {
                    parentElement.findParentOfType<DtoPsiMacro>()?.also { macro ->
                        val currentImmutableType = findCurrentImmutableType(macro) ?: return null
                        val find = currentImmutableType.superTypes().find { it.name() == text }
                        return facade.findClass(
                            if (text == "this") currentImmutableType.qualifiedName() else find?.qualifiedName()
                                ?: return null,
                            project.allScope()
                        )
                    }
                } else if (parentElement.findParentOfType<DtoPsiImportStatement>() == null) {
                    var reference = facade.findClass(parentElement.qualifiedName, project.allScope())

                    if (reference == null) {
                        findParentOfType<DtoPsiRoot>()?.importStatements?.forEach { importStatement ->
                            val qualifiedName = importStatement.qualifiedNameParts?.qualifiedName
                            qualifiedName?.takeIf { it.endsWith(parentElement.qualifiedName) }
                                ?.also {
                                    reference = facade.findClass(it, project.allScope())
                                }

                            importStatement.importTypes.forEach { importType ->
                                if (reference == null) {
                                    reference = facade.findClass(
                                        "$qualifiedName.${importType.name?.value}",
                                        project.allScope()
                                    )
                                }
                            }
                        }
                    }

                    reference
                } else {
                    null
                }
            } else {
                facade
                    .findPackage(
                        parentElement.parts.subList(0, parentElement.parts.indexOf(this) + 1)
                            .joinToString(".") { it.text })
            }
        }

        if (parentElement is DtoPsiPackageParts) {
            parentElement.qualifiedNameParts?.also { qualifiedNameParts ->
                return facade
                    .findPackage(
                        qualifiedNameParts.parts.subList(0, qualifiedNameParts.parts.indexOf(this) + 1)
                            .joinToString(".") { it.text })
            }
        }
        return null
    }
}