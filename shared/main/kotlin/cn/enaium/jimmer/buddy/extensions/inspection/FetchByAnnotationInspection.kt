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

import cn.enaium.jimmer.buddy.utility.classLiteral
import cn.enaium.jimmer.buddy.utility.string
import com.intellij.codeInspection.LocalInspectionTool
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
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.uast.*

/**
 * @author Enaium
 */
class FetchByAnnotationInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
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
                        }?.fields?.firstOrNull { field -> field.hasExplicitModifier(PsiModifier.STATIC) && field.name == value }
                            ?.also {
                                val canonicalText = it.type.canonicalText
                                if (canonicalText.startsWith(Fetcher::class.qualifiedName!!) == false) {
                                    holder.registerProblem(element, "The property type is not a fetcher")
                                } else if (canonicalText.substringAfter("<")
                                        .substringBefore(">") != (element.parent as? PsiTypeElement)?.type?.canonicalText
                                ) {
                                    holder.registerProblem(element, "The fetcher type is not match")
                                }
                            } ?: run {
                            holder.registerProblem(element, "The fetch field is not found")
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
                        }?.companionObjects?.firstOrNull { companionObject ->
                            companionObject.body?.properties?.firstOrNull { property -> property.name == value }.also {
                                val canonicalText = (it.toUElement() as? UField)?.type?.canonicalText
                                if (canonicalText?.startsWith(Fetcher::class.qualifiedName!!) == false) {
                                    holder.registerProblem(element, "The property type is not a fetcher")
                                } else if (canonicalText?.substringAfter("<")?.substringBefore(">")
                                    != element.getParentOfType<KtTypeReference>(true)
                                        .toUElementOfType<UTypeReferenceExpression>()?.type?.canonicalText
                                ) {
                                    holder.registerProblem(element, "The fetcher type is not match")
                                }
                            } !== null
                        } ?: run {
                            holder.registerProblem(element, "The fetch property is not found")
                        }
                    }
                }
            }
        }
    }
}