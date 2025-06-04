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

import cn.enaium.jimmer.buddy.extensions.dto.completion.getTrace
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoType
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiEnumMapping
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiImportStatement
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiImportedType
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiName
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiNamedElement
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiPositiveProp
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiRoot
import cn.enaium.jimmer.buddy.utility.isJavaProject
import cn.enaium.jimmer.buddy.utility.isKotlinProject
import cn.enaium.jimmer.buddy.utility.toCommonImmutableType
import cn.enaium.jimmer.buddy.utility.toImmutable
import com.intellij.lang.ASTNode
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.util.findParentOfType
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
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
                val dtoPsiRoot = parentElement.findParentOfType<DtoPsiRoot>() ?: return null
                val exportType = dtoPsiRoot.qualifiedName() ?: return null
                val exportPackage = dtoPsiRoot.exportStatement?.packageParts?.qualifiedName
                    ?: "${exportType.substringBeforeLast(".")}.dto"
                JavaPsiFacade.getInstance(parentElement.project)
                    .findClass("$exportPackage.$name", parentElement.project.allScope())
            }

            is DtoPsiEnumMapping -> {
                val prop = parentElement.findParentOfType<DtoPsiPositiveProp>() ?: return null
                val trace = getTrace(prop)
                val typeName =
                    parentElement.findParentOfType<DtoPsiRoot>()?.qualifiedName()
                        ?: return null
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

                val enumName =
                    currentImmutable.props().find { it.name() == prop.prop?.value }?.typeName() ?: return null

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