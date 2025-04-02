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
import cn.enaium.jimmer.buddy.utility.annotArgName
import cn.enaium.jimmer.buddy.utility.getTarget
import cn.enaium.jimmer.buddy.utility.method
import cn.enaium.jimmer.buddy.utility.property
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

/**
 * @author Enaium
 */
object MappedByPsiReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<out PsiReference> {
        return arrayOf(Reference(element))
    }

    private class Reference(val e: PsiElement) : PsiReferenceBase<PsiElement>(e) {

        val props = getProps(e)

        override fun resolve(): PsiElement? {
            val text = e.text
            return props[text.substring(1, text.length - 1)]
        }

        override fun getVariants(): Array<out Any> {
            return props.keys.map { LookupElementBuilder.create(it).withIcon(JimmerBuddy.Icons.PROP) }.toTypedArray()
        }

        private fun getProps(element: PsiElement): Map<String, PsiElement> {
            if (element.annotArgName() != "mappedBy") {
                return emptyMap()
            }

            val result = mutableMapOf<String, PsiElement>()
            element.getParentOfType<PsiAnnotation>(true)?.method()?.getTarget()?.methods?.forEach {
                result[it.name] = it
            }
            element.getParentOfType<KtAnnotationEntry>(true)?.property()?.getTarget()?.getProperties()?.forEach {
                result[it.name ?: "Unknown name"] = it
            }
            return result
        }
    }
}