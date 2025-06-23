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

import cn.enaium.jimmer.buddy.utility.*
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.psi.KtProperty

/**
 * @author Enaium
 */
class AssociationAnnotationInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {

                if (!element.inImmutable()) {
                    return
                }

                val toManyProblem = I18n.message("inspection.annotation.association.collectionToOne")
                val noToOneProblem = I18n.message("inspection.annotation.association.withoutToOne")
                val toOneProblem = I18n.message("inspection.annotation.association.nonCollectionToMany")
                val noToManyProblem = I18n.message("inspection.annotation.association.withoutToMany")

                if (element is PsiMethod) {
                    val returnPsiClass = element.returnType?.let { PsiUtil.resolveGenericsClassInType(it) }
                    if (returnPsiClass?.substitutor != PsiSubstitutor.EMPTY && listOf(
                            List::class.java.name,
                            Set::class.java.name,
                            Collection::class.java.name
                        ).any { it == returnPsiClass?.element?.qualifiedName }
                    ) {
                        if (element.hasToOneAnnotation()) {
                            holder.registerProblem(element, toManyProblem)
                        } else if (returnPsiClass?.element?.typeParameters?.firstOrNull()
                                ?.let { returnPsiClass.substitutor.substitute(it) }
                                ?.let { PsiUtil.resolveGenericsClassInType(it) }
                                ?.element?.isEntity() == true && !element.hasToManyAnnotation()
                            && !element.hasTransientAnnotation()
                        ) {
                            holder.registerProblem(element, noToManyProblem)
                        }
                    } else {
                        if (element.hasToManyAnnotation()) {
                            holder.registerProblem(element, toOneProblem)
                        } else if (returnPsiClass?.element?.isEntity() == true && !element.hasToOneAnnotation() && !element.hasTransientAnnotation() && !element.hasManyToManyViewAnnotation()) {
                            holder.registerProblem(element, noToOneProblem)
                        }
                    }
                } else if (element is KtProperty) {
                    val typeReference = element.typeReference?.type()
                    if (typeReference?.arguments?.isNotEmpty() == true && listOf(
                            List::class.qualifiedName,
                            Set::class.qualifiedName,
                            Collection::class.qualifiedName
                        ).any { it == typeReference.ktClass?.fqName?.asString() }
                    ) {
                        if (element.hasToOneAnnotation()) {
                            holder.registerProblem(element, toManyProblem)
                        } else if (typeReference.arguments.firstOrNull()?.ktClass?.isEntity() == true && !element.hasToManyAnnotation() && !element.hasTransientAnnotation() && !element.hasManyToManyViewAnnotation()) {
                            holder.registerProblem(element, noToManyProblem)
                        }
                    } else {
                        if (element.hasToManyAnnotation()) {
                            holder.registerProblem(element, toOneProblem)
                        } else if (typeReference?.ktClass?.isEntity() == true && !element.hasToOneAnnotation() && !element.hasTransientAnnotation()) {
                            holder.registerProblem(element, noToOneProblem)
                        }
                    }
                }
            }
        }
    }
}