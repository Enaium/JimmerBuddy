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

package cn.enaium.jimmer.buddy.inspection

import cn.enaium.jimmer.buddy.JimmerBuddy.PSI_SHARED
import cn.enaium.jimmer.buddy.utility.hasIdViewAnnotation
import cn.enaium.jimmer.buddy.utility.toAny
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.util.PsiUtil
import org.babyfish.jimmer.sql.IdView
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClass

/**
 * @author Enaium
 */
class IdViewAnnotationInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val noValue = "The prop type is a collection, but the annotation hasn't value"
                val basePropNotExists = "The base prop doesn't exists"
                val propNotCollection = "The prop type has generic, but the prop type isn't a collection"

                if (element is PsiMethod && element.hasIdViewAnnotation()) {
                    val returnPsiClass = element.returnType?.let { PsiUtil.resolveGenericsClassInType(it) }
                    if (returnPsiClass?.substitutor != PsiSubstitutor.EMPTY) {
                        if (!listOf(
                                List::class.java.name,
                                Set::class.java.name,
                                Collection::class.java.name
                            ).any { it == returnPsiClass?.element?.qualifiedName }
                        ) {
                            holder.registerProblem(element, propNotCollection)
                        }
                        element.modifierList.annotations.find { it.qualifiedName == IdView::class.qualifiedName }
                            ?.findAttributeValue("value")?.toAny(String::class.java)?.toString()
                            ?.takeIf { it.isNotBlank() }?.also { value ->
                                element.containingClass?.also { c ->
                                    if (c.methods.none { it.name == value }) {
                                        holder.registerProblem(element, basePropNotExists)
                                    }
                                }
                            } ?: holder.registerProblem(element, noValue)
                    } else {

                        var baseProp: String? = null

                        baseProp =
                            element.modifierList.annotations.find { it.qualifiedName == IdView::class.qualifiedName }
                                ?.findAttributeValue("value")?.toAny(String::class.java)?.toString()
                                ?.takeIf { it.isNotBlank() }

                        if (baseProp == null && element.name.endsWith("Id")) {
                            baseProp = element.name.substring(0, element.name.length - 2)
                        }

                        element.containingClass?.also { c ->
                            if (c.methods.none { it.name == baseProp }) {
                                holder.registerProblem(element, basePropNotExists)
                            }
                        }
                    }
                } else if (element is KtProperty && element.hasIdViewAnnotation()) {
                    val typeReference = element.typeReference?.let { PSI_SHARED.type(it) }
                    if (typeReference?.arguments?.isNotEmpty() == true) {
                        if (!listOf(
                                List::class.qualifiedName,
                                Set::class.qualifiedName,
                                Collection::class.qualifiedName
                            ).any { it == typeReference.ktClass?.fqName?.asString() }
                        ) {
                            holder.registerProblem(element, propNotCollection)
                        }
                        PSI_SHARED.annotations(element)
                            .find { it.fqName == IdView::class.qualifiedName }?.arguments?.find { it.name == "value" }
                            ?.also {
                                val value = it.value?.toString() ?: ""

                                if (value.isBlank()) {
                                    holder.registerProblem(element, noValue)
                                }

                                element.containingClass()?.also { c ->
                                    if (c.getProperties().none { it.name == value }) {
                                        holder.registerProblem(element, basePropNotExists)
                                    }
                                }
                            } ?: holder.registerProblem(element, noValue)
                    } else {

                        var baseProp: String? = null

                        baseProp = PSI_SHARED.annotations(element)
                            .find { it.fqName == IdView::class.qualifiedName }?.arguments?.find { it.name == "value" }?.value?.toString()

                        if (baseProp == null && element.name?.endsWith("Id") == true) {
                            baseProp = element.name!!.substring(0, element.name!!.length - 2)
                        }

                        element.containingClass()?.also { c ->
                            if (c.getProperties().none { it.name == baseProp }) {
                                holder.registerProblem(element, basePropNotExists)
                            }
                        }
                    }
                }
            }
        }
    }
}