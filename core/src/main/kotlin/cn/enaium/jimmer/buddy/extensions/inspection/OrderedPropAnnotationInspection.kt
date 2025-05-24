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
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.babyfish.jimmer.sql.OrderedProp
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.toUElementOfType

/**
 * @author Enaium
 */
class OrderedPropAnnotationInspection : AbstractLocalInspectionTool() {
    override fun visit(element: PsiElement, holder: ProblemsHolder, isOnTheFly: Boolean) {
        if (!element.inImmutable()) {
            return
        }

        if (element is PsiAnnotation && toManyAnnotations.contains(element.getParentOfType<PsiAnnotation>(true)?.qualifiedName)
            && element.qualifiedName == OrderedProp::class.qualifiedName
        ) {
            val name = element.toUElementOfType<UAnnotation>()?.findAttributeValue("value")?.string() ?: return
            element.getParentOfType<PsiMethod>(true)?.getTarget()?.allMethods?.find { it.name == name }
                ?: holder.registerProblem(element, "The property is not found")
        }

        if ((element is KtAnnotationEntry || element is KtCallExpression) && toManyAnnotations.contains(
                element.getParentOfType<KtAnnotationEntry>(true)?.toUElementOfType<UAnnotation>()?.qualifiedName
            )
            && element.toUElementOfType<UAnnotation>()?.qualifiedName == OrderedProp::class.qualifiedName
        ) {
            val name = element.toUElementOfType<UAnnotation>()?.findAttributeValue("value")?.string() ?: return
            element.getParentOfType<KtProperty>(true)?.getTarget()?.getAllProperties()?.find { it.name == name }
                ?: holder.registerProblem(element, "The property is not found")
        }
    }
}