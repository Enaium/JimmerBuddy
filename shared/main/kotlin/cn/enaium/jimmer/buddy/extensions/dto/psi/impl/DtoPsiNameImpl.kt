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

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoType
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiImportStatement
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiImportedType
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiName
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiNamedElement
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiRoot
import com.intellij.lang.ASTNode
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.util.findParentOfType
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class DtoPsiNameImpl(node: ASTNode) : DtoPsiNamedElement(node), DtoPsiName {
    override val value: String
        get() = node.text

    override fun getName(): String = value

    override fun reference(): PsiElement? {
        val parentElement = parent
        return when (parentElement) {
            is DtoPsiImportedType -> {
                JavaPsiFacade.getInstance(project).findClass(
                    "${parentElement.findParentOfType<DtoPsiImportStatement>()?.qualifiedNameParts?.qualifiedName}.$value",
                    project.allScope()
                )
            }

            is DtoPsiDtoType -> {
                val name = parentElement.name?.value ?: return null
                val dtoPsiRoot = parentElement.findParentOfType<DtoPsiRoot>() ?: return null
                val exportType = dtoPsiRoot.exportStatement?.typeParts?.qualifiedName ?: return null
                val exportPackage = dtoPsiRoot.exportStatement?.packageParts?.qualifiedName
                    ?: "${exportType.substringBeforeLast(".")}.dto"
                JavaPsiFacade.getInstance(parentElement.project)
                    .findClass("$exportPackage.$name", parentElement.project.allScope())
            }

            else -> null
        }
    }
}