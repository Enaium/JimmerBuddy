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
import cn.enaium.jimmer.buddy.utility.isJavaProject
import cn.enaium.jimmer.buddy.utility.isKotlinProject
import com.intellij.lang.ASTNode
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.util.findParentOfType
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

class DtoPsiNameImpl(node: ASTNode) : DtoPsiNamedElement(node), DtoPsiName {
    override val value: String
        get() = this.text

    override fun getName(): String = value

    override fun reference(): PsiElement? {
        return when (val parentElement = parent) {
            is DtoPsiImportedType -> {
                JavaPsiFacade.getInstance(project).findClass(
                    "${parentElement.findParentOfType<DtoPsiImportStatement>()?.qualifiedNameParts?.qualifiedName}.$value",
                    project.allScope()
                )
            }

            is DtoPsiDtoType -> {
                val dtoPsiRoot = parentElement.findParentOfType<DtoPsiRoot>() ?: return null
                val exportType = dtoPsiRoot.qualifiedName() ?: return null
                val exportPackage = dtoPsiRoot.exportStatement?.packageParts?.qualifiedName()
                    ?: "${exportType.substringBeforeLast(".")}.dto"
                JavaPsiFacade.getInstance(parentElement.project)
                    .findClass("$exportPackage.$name", parentElement.project.allScope())
            }

            is DtoPsiEnumMapping -> {
                val prop = parentElement.findParentOfType<DtoPsiPositiveProp>() ?: return null
                val enumName =
                    findCurrentImmutableType(prop)?.props()?.find { it.name() == prop.prop?.value }?.typeName()
                        ?: return null

                return if (project.isJavaProject()) {
                    JavaPsiFacade.getInstance(project).findClass(enumName, project.allScope())
                        ?.getChildrenOfType<PsiEnumConstant>()
                        ?.find { it.name == name }
                } else if (project.isKotlinProject()) {
                    (KotlinFullClassNameIndex[enumName, project, project.allScope()].firstOrNull() as? KtClass)?.getChildOfType<KtClassBody>()
                        ?.getChildrenOfType<KtEnumEntry>()?.find { it.name == name }
                } else {
                    null
                }
            }

            else -> null
        }
    }
}