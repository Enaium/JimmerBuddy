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
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

/**
 * @author Enaium
 */
class MappedByInspection : AbstractLocalInspectionTool() {
    override fun visit(
        element: PsiElement,
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ) {
        if (!element.inImmutable()) {
            return
        }

        if (!mappedByAnnotations.any { element.isAnnotation(it) }) return

        element.getParentOfType<PsiMethod>(true)?.also { methodElement ->
            if (element is PsiAnnotation) {
                val mappedBy = element.findAttributeValue("mappedBy")?.toAny(String::class.java)?.toString()
                    ?.takeIf { it.isNotBlank() } ?: return@also
                methodElement.getTarget()?.also {
                    it.allMethods.find { method -> method.name == mappedBy }?.also {
                        if (it.getTarget() != methodElement.containingClass) {
                            holder.registerProblem(
                                element,
                                I18n.message("inspection.annotation.mappedBy.propTypeNotMatch")
                            )
                        }
                    } ?: holder.registerProblem(
                        element,
                        I18n.message("inspection.annotation.mappedBy.propNotFound")
                    )
                }
            }
        }

        element.getParentOfType<KtProperty>(true)?.also { propertyElement ->
            propertyElement.annotations().find { mappedByAnnotations.contains(it.fqName) }
                ?.also {
                    val mappedBy =
                        it.findArgument("mappedBy")?.value?.toString()
                            ?: return@also
                    propertyElement.getTarget()?.also { ktClass ->
                        ktClass.findPropertyByName(mappedBy, true)?.also { namedDeclaration ->
                            if ((namedDeclaration as? KtProperty)?.getTarget() != propertyElement.containingClass()) {
                                holder.registerProblem(
                                    element,
                                    "The mappedBy prop type is not match"
                                )
                            }
                        } ?: holder.registerProblem(
                            element,
                            "The mappedBy prop is not found"
                        )
                    }
                }
        }
    }
}