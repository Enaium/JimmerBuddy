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
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod
import org.babyfish.jimmer.Formula
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClass

/**
 * @author Enaium
 */
class FormulaAnnotationInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: com.intellij.psi.PsiElement) {
                if (element is PsiMethod && element.containingClass?.hasImmutableAnnotation() == true) {
                    element.annotations.find { it.qualifiedName == Formula::class.qualifiedName }?.also {
                        val dependencies = (it.findAttributeValue("dependencies")
                            ?.toAny(Array<String>::class.java) as? Array<*>)?.map { it.toString() }
                            ?.takeIf { it.isNotEmpty() } ?: run {
                            element.body?.also {
                                holder.registerProblem(
                                    element,
                                    "The dependencies is empty"
                                )
                            }
                            return@also
                        }
                        dependencies.forEach { dependency ->
                            val trace = dependency.split(".")
                            var containingClass = element.containingClass

                            trace.forEach {
                                containingClass?.findMethodsByName(it, true)?.takeIf { it.isNotEmpty() }?.also {
                                    containingClass = it.first().getTarget()
                                } ?: run {
                                    holder.registerProblem(element, "The dependency '$it' does not exist")
                                    return@also
                                }
                            }
                        }
                    }
                } else if (element is KtProperty && element.containingClass()?.hasImmutableAnnotation() == true) {
                    element.annotations().find { it.fqName == Formula::class.qualifiedName }
                        ?.also {
                            val dependencies =
                                (it.arguments.find { argument -> argument.name == "dependencies" }?.value as? List<*>)?.map { it.toString() }
                                    ?: run {
                                        element.getter?.also {
                                            holder.registerProblem(
                                                element,
                                                "The dependencies is empty"
                                            )
                                        }
                                        return@also
                                    }
                            dependencies.forEach { dependency ->
                                val trace = dependency.split(".")
                                var containingClass = element.containingClass()
                                trace.forEach {
                                    containingClass?.findPropertyByName(it, true)?.also {
                                        containingClass = (it as KtProperty).getTarget()
                                    } ?: run {
                                        holder.registerProblem(element, "The dependency '$it' does not exist")
                                        return@also
                                    }
                                }
                            }
                        }
                }
            }
        }
    }
}