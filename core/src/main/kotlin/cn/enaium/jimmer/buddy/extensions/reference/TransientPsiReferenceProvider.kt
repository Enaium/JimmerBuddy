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
import cn.enaium.jimmer.buddy.utility.PROP
import cn.enaium.jimmer.buddy.utility.annotArgName
import cn.enaium.jimmer.buddy.utility.annotName
import cn.enaium.jimmer.buddy.utility.findSpringBeanTargets
import cn.enaium.jimmer.buddy.utility.subMiddle
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import org.babyfish.jimmer.sql.Transient

/**
 * @author Enaium
 */
object TransientPsiReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<out PsiReference> {
        if (element.annotName() != Transient::class.qualifiedName) {
            return emptyArray()
        }

        if (element.annotArgName() != "ref") {
            return emptyArray()
        }

        return arrayOf(Reference(element))
    }

    private class Reference(e: PsiElement) : PsiReferenceBase<PsiElement>(e) {
        private val beanName = e.text.subMiddle("\"", "\"")
        private val targets = e.project.findSpringBeanTargets(beanName)

        override fun resolve(): PsiElement? {
            return targets.firstOrNull()
        }

        override fun getVariants(): Array<out Any> {
            return targets.map { target ->
                LookupElementBuilder.create((target as? PsiNamedElement)?.name ?: beanName).withIcon(JimmerBuddy.Icons.PROP)
            }.toTypedArray()
        }
    }
}
