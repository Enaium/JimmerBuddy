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
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClass

/**
 * @author Enaium
 */
class MappedByInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is PsiMethod && element.containingClass?.hasImmutableAnnotation() == true) {
                    element.modifierList.annotations.find { mappedByAnnotations.contains(it.qualifiedName) }
                        ?.also {
                            val mappedBy = it.findAttributeValue("mappedBy")?.toAny(String::class.java)?.toString()
                                ?.takeIf { it.isNotBlank() } ?: return@also
                            element.getTarget()?.also {
                                it.methods.find { method -> method.name == mappedBy }?.also {
                                    if (it.getTarget() != element.containingClass) {
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
                } else if (element is KtProperty && element.containingClass()?.hasImmutableAnnotation() == true) {
                    element.annotations().find { mappedByAnnotations.contains(it.fqName) }
                        ?.also {
                            val mappedBy =
                                it.arguments.find { argument -> argument.name == "mappedBy" }?.value?.toString()
                                    ?: return@also
                            element.getTarget()?.also {
                                it.getProperties().find { property -> property.name == mappedBy }?.also {
                                    if (it.getTarget() != element.containingClass()) {
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
    }
}