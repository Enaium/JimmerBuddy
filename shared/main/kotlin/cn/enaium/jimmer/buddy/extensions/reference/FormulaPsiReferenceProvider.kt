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

package cn.enaium.jimmer.buddy.extensions.reference

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.utility.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.babyfish.jimmer.Formula
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

/**
 * @author Enaium
 */
object FormulaPsiReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<out PsiReference> {
        return arrayOf(Reference(element))
    }

    private class Reference(e: PsiElement) : PsiReferenceBase<PsiElement>(e) {

        val text = e.text.let { it.substring(1, it.length - 1) }

        val props = getProps(e)

        override fun resolve(): PsiElement? {
            return props[text]
        }

        override fun getVariants(): Array<out Any> {
            return props.keys.map { LookupElementBuilder.create(it).withIcon(JimmerBuddy.Icons.PROP) }.toTypedArray()
        }

        private fun getProps(element: PsiElement): Map<String, PsiElement> {
            if (element.annotName() != Formula::class.qualifiedName) {
                return emptyMap()
            }

            if (element.annotArgName() != "dependencies") {
                return emptyMap()
            }

            val result = mutableMapOf<String, PsiElement>()
            val trace = text.split(".")

            element.getParentOfType<PsiClass>(true)?.also {
                if (trace.size == 1) {
                    it.allMethods.forEach { method ->
                        if (method.containingClass?.isImmutable() == true) {
                            result[method.name] = method
                        }
                    }
                } else {
                    var currentClass = it
                    trace.forEachIndexed { index, name ->
                        if (index == trace.size - 1) {
                            return@forEachIndexed
                        }

                        currentClass.findMethodsByName(name, true).takeIf { it.isNotEmpty() }?.also {
                            currentClass = it.first().getTarget() ?: return@forEachIndexed
                        }
                    }
                    currentClass.allMethods.forEach { method ->
                        if (method.containingClass?.isImmutable() == true) {
                            result["${trace.subList(0, trace.size - 1).joinToString(".")}.${method.name}"] = method
                        }
                    }
                }
            }

            element.getParentOfType<KtClass>(true)?.also {
                if (trace.size == 1) {
                    it.getAllProperties().forEach { property ->
                        result[property.name ?: "Unknown name"] = property
                    }
                } else {
                    var currentClass = it
                    trace.forEachIndexed { index, name ->
                        if (index == trace.size - 1) {
                            return@forEachIndexed
                        }

                        currentClass.findPropertyByName(name, true)?.also {
                            (it as? KtProperty)?.getTarget()?.also {
                                currentClass = it
                            }
                        }
                    }
                    currentClass.getAllProperties().forEach { property ->
                        result["${trace.subList(0, trace.size - 1).joinToString(".")}.${property.name}"] = property
                    }
                }
            }

            return result
        }
    }
}