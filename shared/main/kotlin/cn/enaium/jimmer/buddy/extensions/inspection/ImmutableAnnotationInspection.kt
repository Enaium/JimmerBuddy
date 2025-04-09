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

import cn.enaium.jimmer.buddy.utility.hasImmutableAnnotation
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClass

/**
 * @author Enaium
 */
class ImmutableAnnotationInspection : AbstractLocalInspectionTool() {
    override fun visit(
        element: PsiElement,
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ) {
        val problem = if (element is PsiClass && element.hasImmutableAnnotation() && !element.isInterface) {
            true
        } else if (element is KtClass && element.hasImmutableAnnotation() && !element.isInterface()) {
            true
        } else {
            false
        }
        if (problem) {
            holder.registerProblem(
                element,
                "You can not use @Immutable annotation on here because Immutable must be an interface."
            )
        }
    }
}