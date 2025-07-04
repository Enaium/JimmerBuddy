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

package cn.enaium.jimmer.buddy.extensions.inspection

import cn.enaium.jimmer.buddy.utility.I18n
import cn.enaium.jimmer.buddy.utility.classLiteral
import cn.enaium.jimmer.buddy.utility.method
import cn.enaium.jimmer.buddy.utility.string
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.intellij.refactoring.extractMethod.newImpl.ExtractMethodHelper.hasExplicitModifier
import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.client.meta.DefaultFetcherOwner
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.uast.*

/**
 * @author Enaium
 */
class FetchByAnnotationInspection : AbstractLocalInspectionTool() {
    override fun visit(
        element: PsiElement,
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ) {
        val u = element.toUElementOfType<UAnnotation>()
        if (u?.qualifiedName != FetchBy::class.qualifiedName) {
            return
        }

        val value = u?.findAttributeValue("value")?.string() ?: return
        var ownerType = u.findAttributeValue("ownerType")?.classLiteral()
        if (element is PsiAnnotation) {
            element.getParentOfType<PsiClass>(true)?.also { klass ->

                if (ownerType == null) {
                    ownerType =
                        (klass.annotations.find { it.qualifiedName == DefaultFetcherOwner::class.qualifiedName })?.toUElementOfType<UAnnotation>()
                            ?.findAttributeValue("value")?.classLiteral()
                }

                if (ownerType == null) {
                    klass
                } else {
                    JavaPsiFacade.getInstance(element.project).findClass(ownerType, element.project.allScope())
                }?.fields?.firstOrNull { field -> (field.hasExplicitModifier(PsiModifier.STATIC) || field.containingClass?.isInterface == true) && field.name == value }
                    ?.also {
                        val canonicalText = it.type.canonicalText
                        if (!canonicalText.startsWith(Fetcher::class.qualifiedName!!)) {
                            holder.registerProblem(element, I18n.message("inspection.annotation.fetchBy.notFetcher"))
                        } else {
                            val typeParameter = canonicalText.substringAfter("<")
                                .substringBefore(">")
                            if (typeParameter != (element.parent as? PsiTypeElement)?.type?.canonicalText && typeParameter != element.method()?.returnType?.canonicalText) {
                                holder.registerProblem(
                                    element,
                                    I18n.message("inspection.annotation.fetchBy.typeNotMatch")
                                )
                            }
                        }
                    } ?: run {
                    holder.registerProblem(element, I18n.message("inspection.annotation.fetchBy.fieldNotFound"))
                }
            }
        } else if (element is KtAnnotationEntry) {
            element.getParentOfType<KtClass>(true)?.also { ktClass ->

                if (ownerType == null) {
                    ownerType =
                        (ktClass.annotationEntries.find { it.toUElementOfType<UAnnotation>()?.qualifiedName == DefaultFetcherOwner::class.qualifiedName })?.toUElementOfType<UAnnotation>()
                            ?.findAttributeValue("value")?.classLiteral()
                }

                if (ownerType == null) {
                    ktClass
                } else {
                    KotlinFullClassNameIndex[ownerType, element.project, element.project.allScope()].firstOrNull()
                }?.also { ktClass ->
                    (ktClass.companionObjects.firstOrNull()?.body?.properties
                        ?: ktClass.takeIf { it is KtObjectDeclaration }?.body?.properties)?.firstOrNull { property -> property.name == value }
                        .also {
                            val canonicalText = (it.toUElement() as? UField)?.type?.canonicalText
                            if (canonicalText?.startsWith(Fetcher::class.qualifiedName!!) == false) {
                                holder.registerProblem(
                                    element,
                                    I18n.message("inspection.annotation.fetchBy.notFetcher")
                                )
                            } else if (canonicalText?.substringAfter("<")?.substringBefore(">")
                                != element.getParentOfType<KtTypeReference>(true)
                                    .toUElementOfType<UTypeReferenceExpression>()?.type?.canonicalText
                            ) {
                                holder.registerProblem(
                                    element,
                                    I18n.message("inspection.annotation.fetchBy.typeNotMatch")
                                )
                            }
                        } ?: holder.registerProblem(
                        element,
                        I18n.message("inspection.annotation.fetchBy.fieldNotFound")
                    )
                }
            }
        }
    }
}