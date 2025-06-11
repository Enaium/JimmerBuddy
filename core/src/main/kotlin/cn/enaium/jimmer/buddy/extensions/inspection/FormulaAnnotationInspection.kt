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
import org.babyfish.jimmer.Formula
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

/**
 * @author Enaium
 */
class FormulaAnnotationInspection : AbstractLocalInspectionTool() {
    override fun visit(
        element: PsiElement,
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ) {
        if (!element.inImmutable()) {
            return
        }

        if (!element.isAnnotation(Formula::class.qualifiedName!!)) return

        if (element is PsiAnnotation) {
            element.getParentOfType<PsiMethod>(true).also { methodElement ->
                val dependencies = (element.findAttributeValue("dependencies")
                    ?.let {
                        it.toAny(Array<String>::class.java) as? Array<*> ?: it.toAny(String::class.java)
                            ?.let { string -> arrayOf(string.toString()) }
                    })?.map { it.toString() }
                    ?.takeIf { it.isNotEmpty() } ?: run {
                    methodElement?.body?.also {
                        holder.registerProblem(
                            element,
                            "The dependencies is empty"
                        )
                    }
                    return
                }

                dependencies.forEach { dependency ->
                    val trace = dependency.split(".")
                    var containingClass = methodElement?.containingClass

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
        }

        if (element is KtAnnotationEntry) {
            element.getParentOfType<KtProperty>(true)?.also { propertyElement ->
                val dependencies =
                    (propertyElement.annotations().find { it.fqName == Formula::class.qualifiedName }
                        ?.findArgument("dependencies")?.value as? List<*>)?.map { it.toString() }
                        ?: run {
                            propertyElement.getter?.also {
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