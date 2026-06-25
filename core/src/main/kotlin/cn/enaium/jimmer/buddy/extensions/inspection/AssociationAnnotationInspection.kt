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
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiUtil
import org.babyfish.jimmer.sql.ManyToMany
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.OneToMany
import org.babyfish.jimmer.sql.OneToOne
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory

/**
 * @author Enaium
 */
class AssociationAnnotationInspection : AbstractLocalInspectionTool() {

    private val toOneQuickFixes = arrayOf(
        AddAssociationAnnotationQuickFix(OneToOne::class.qualifiedName!!, OneToOne::class.simpleName!!),
        AddAssociationAnnotationQuickFix(ManyToOne::class.qualifiedName!!, ManyToOne::class.simpleName!!)
    )

    private val toManyQuickFixes = arrayOf(
        AddAssociationAnnotationQuickFix(OneToMany::class.qualifiedName!!, OneToMany::class.simpleName!!),
        AddAssociationAnnotationQuickFix(ManyToMany::class.qualifiedName!!, ManyToMany::class.simpleName!!)
    )

    override fun visit(
        element: PsiElement,
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ) {
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
                    && !element.isComputed() && !element.hasManyToManyViewAnnotation()
                ) {
                    holder.registerProblem(element, noToManyProblem, *toManyQuickFixes)
                }
            } else {
                if (element.hasToManyAnnotation()) {
                    holder.registerProblem(element, toOneProblem)
                } else if (returnPsiClass?.element?.isEntity() == true && !element.hasToOneAnnotation() && !element.isComputed()) {
                    holder.registerProblem(element, noToOneProblem, *toOneQuickFixes)
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
                } else if (typeReference.firstArgType()?.ktClass?.isEntity() == true && !element.hasToManyAnnotation() && !element.isComputed() && !element.hasManyToManyViewAnnotation()) {
                    holder.registerProblem(element, noToManyProblem, *toManyQuickFixes)
                }
            } else {
                if (element.hasToManyAnnotation()) {
                    holder.registerProblem(element, toOneProblem)
                } else if (typeReference?.ktClass?.isEntity() == true && !element.hasToOneAnnotation() && !element.isComputed()) {
                    holder.registerProblem(element, noToOneProblem, *toOneQuickFixes)
                }
            }
        }
    }
}

private class AddAssociationAnnotationQuickFix(
    private val annotationQualifiedName: String,
    private val annotationSimpleName: String
) : LocalQuickFix {
    override fun getName(): String {
        return I18n.message("intention.annotation.association.add", annotationSimpleName)
    }

    override fun getFamilyName(): String {
        return name
    }

    override fun applyFix(
        project: Project,
        descriptor: ProblemDescriptor
    ) {
        val element = descriptor.psiElement
        if (element is PsiMethod) {
            if (element.modifierList.annotations.none { it.qualifiedName == annotationQualifiedName }) {
                val annotation = element.modifierList.addAnnotation(annotationQualifiedName)
                JavaCodeStyleManager.getInstance(project).shortenClassReferences(annotation)
            }
        } else if (element is KtProperty) {
            if (element.annotations().none { it.fqName == annotationQualifiedName }) {
                val annotation = element.addAnnotationEntry(
                    KtPsiFactory(project).createAnnotationEntry("@$annotationQualifiedName")
                )
                ShortenReferences.DEFAULT.process(annotation)
            }
        }
    }
}
