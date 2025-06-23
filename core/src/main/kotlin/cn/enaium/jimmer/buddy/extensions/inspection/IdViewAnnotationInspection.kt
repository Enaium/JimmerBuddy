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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.util.PsiUtil
import org.babyfish.jimmer.sql.IdView
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

/**
 * @author Enaium
 */
class IdViewAnnotationInspection : AbstractLocalInspectionTool() {
    override fun visit(
        element: PsiElement,
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ) {
        if (!element.inImmutable()) {
            return
        }

        val noValue = I18n.message("inspection.annotation.idView.withoutValue")
        val basePropNotExists = I18n.message("inspection.annotation.idView.basePropNotExist")
        val propNotCollection = I18n.message("inspection.annotation.idView.propIsNonCollection")

        if (!element.isAnnotation(IdView::class.qualifiedName!!)) return
        element.getParentOfType<PsiMethod>(true)?.also { methodElement ->
            val returnPsiClass = methodElement.returnType?.let { PsiUtil.resolveGenericsClassInType(it) }
            if (returnPsiClass?.substitutor != PsiSubstitutor.EMPTY) {
                if (!listOf(
                        List::class.java.name,
                        Set::class.java.name,
                        Collection::class.java.name
                    ).any { it == returnPsiClass?.element?.qualifiedName }
                ) {
                    holder.registerProblem(methodElement, propNotCollection)
                }
                methodElement.modifierList.annotations.find { it.qualifiedName == IdView::class.qualifiedName }
                    ?.findAttributeValue("value")?.toAny(String::class.java)?.toString()
                    ?.takeIf { it.isNotBlank() }?.also { value ->
                        methodElement.containingClass?.also { c ->
                            if (c.allMethods.none { it.name == value }) {
                                holder.registerProblem(methodElement, basePropNotExists)
                            }
                        }
                    } ?: holder.registerProblem(element, noValue)
            } else {
                var baseProp: String? = null

                baseProp =
                    methodElement.modifierList.annotations.find { it.qualifiedName == IdView::class.qualifiedName }
                        ?.findAttributeValue("value")?.toAny(String::class.java)?.toString()
                        ?.takeIf { it.isNotBlank() }

                if (baseProp == null && methodElement.name.endsWith("Id")) {
                    baseProp = methodElement.name.substring(0, methodElement.name.length - 2)
                }

                methodElement.containingClass?.also { c ->
                    if (c.allMethods.none { it.name == baseProp }) {
                        holder.registerProblem(element, basePropNotExists)
                    }
                }
            }
        }

        element.getParentOfType<KtProperty>(true)?.also { propertyElement ->
            val typeReference = propertyElement.typeReference?.type()
            if (typeReference?.arguments?.isNotEmpty() == true) {
                if (!listOf(
                        List::class.qualifiedName,
                        Set::class.qualifiedName,
                        Collection::class.qualifiedName
                    ).any { it == typeReference.ktClass?.fqName?.asString() }
                ) {
                    holder.registerProblem(element, propNotCollection)
                }
                propertyElement.annotations()
                    .find { it.fqName == IdView::class.qualifiedName }?.findArgument("value")
                    ?.also {
                        val value = it.value?.toString() ?: ""

                        if (value.isBlank()) {
                            holder.registerProblem(element, noValue)
                        }

                        propertyElement.containingClass()?.also { c ->
                            if (c.findPropertyByName(value, true) == null) {
                                holder.registerProblem(element, basePropNotExists)
                            }
                        }
                    } ?: holder.registerProblem(element, noValue)
            } else {
                var baseProp: String? = null

                baseProp = propertyElement.annotations()
                    .find { it.fqName == IdView::class.qualifiedName }?.findArgument("value")?.value?.toString()

                if (baseProp == null && propertyElement.name?.endsWith("Id") == true) {
                    baseProp = propertyElement.name!!.substring(0, propertyElement.name!!.length - 2)
                }

                propertyElement.containingClass()?.also { c ->
                    if (c.findPropertyByName(baseProp ?: return, true) == null) {
                        holder.registerProblem(element, basePropNotExists)
                    }
                }
            }
        }
    }
}