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

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiImportStatement
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiNamedElement
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiPackageParts
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiPart
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiQualifiedNameParts
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiRoot
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiTypeParts
import com.intellij.lang.ASTNode
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.util.findParentOfType
import org.jetbrains.kotlin.idea.base.util.allScope

class DtoPsiPartImpl(node: ASTNode) : DtoPsiNamedElement(node), DtoPsiPart {
    override val part: String = node.text
    override fun reference(): PsiElement? {
        val parentElement = parent
        val facade = JavaPsiFacade.getInstance(project)
        if (parentElement is DtoPsiTypeParts) {
            return if (parentElement.parts.last() == this) {
                facade.findClass(parentElement.qualifiedName, project.allScope())
            } else {
                facade
                    .findPackage(
                        parentElement.parts.subList(0, parentElement.parts.indexOf(this) + 1)
                            .joinToString(".") { it.part })
            }
        }

        if (parentElement is DtoPsiQualifiedNameParts) {
            return if (parentElement.parts.last() == this && parentElement.findParentOfType<DtoPsiImportStatement>() == null) {
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
                facade
                    .findPackage(
                        parentElement.parts.subList(0, parentElement.parts.indexOf(this) + 1)
                            .joinToString(".") { it.part })
            }
        }

        if (parentElement is DtoPsiPackageParts) {
            return facade
                .findPackage(
                    parentElement.parts.subList(0, parentElement.parts.indexOf(this) + 1).joinToString(".") { it.part })
        }
        return null
    }
}