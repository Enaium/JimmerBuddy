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
import cn.enaium.jimmer.buddy.utility.annotName
import cn.enaium.jimmer.buddy.utility.annotValue
import cn.enaium.jimmer.buddy.utility.classLiteral
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.*
import com.intellij.refactoring.extractMethod.newImpl.ExtractMethodHelper.hasExplicitModifier
import com.intellij.util.ProcessingContext
import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.client.meta.DefaultFetcherOwner
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isObjectLiteral
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.toUElementOfType

/**
 * @author Enaium
 */
object FetchByPsiReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<out PsiReference> {
        return arrayOf(Reference(element))
    }

    private class Reference(e: PsiElement) : PsiReferenceBase<PsiElement>(e) {

        val text = e.text.substringAfter("\"").substringBefore("\"")

        val props = getProps(e)

        override fun resolve(): PsiElement? {
            return props[text]
        }

        override fun getVariants(): Array<out Any> {
            return props.keys.map { LookupElementBuilder.create(it).withIcon(JimmerBuddy.Icons.DTO) }.toTypedArray()
        }

        private fun getProps(element: PsiElement): Map<String, PsiElement> {
            if (element.annotName() != FetchBy::class.qualifiedName) {
                return emptyMap()
            }

            if (element.annotArgName() != "value") {
                return emptyMap()
            }

            var ownerType = element.annotValue("ownerType")?.classLiteral()

            val result = mutableMapOf<String, PsiElement>()

            element.getParentOfType<PsiClass>(true)?.also { klass ->

                if (ownerType == null) {
                    ownerType =
                        (klass.annotations.find { it.qualifiedName == DefaultFetcherOwner::class.qualifiedName })?.toUElementOfType<UAnnotation>()
                            ?.findAttributeValue("value")?.classLiteral()
                }

                if (ownerType == null) {
                    klass
                } else {
                    JavaPsiFacade.getInstance(element.project).findClass(ownerType, element.project.allScope())
                }?.fields?.forEach { field ->
                    if (field.hasExplicitModifier(PsiModifier.STATIC) || field.containingClass?.isInterface == true) {
                        result[field.name] = field
                    }
                }
            }
            element.getParentOfType<KtClass>(true)?.also { ktClass ->

                if (ownerType == null) {
                    ownerType =
                        (ktClass.annotationEntries.find { it.toUElementOfType<UAnnotation>()?.qualifiedName == DefaultFetcherOwner::class.qualifiedName })?.toUElementOfType<UAnnotation>()
                            ?.findAttributeValue("value")?.classLiteral()
                }

                if (ownerType == null) {
                    ktClass
                } else {
                    KotlinFullClassNameIndex[ownerType, element.project, element.project.allScope()].firstOrNull()
                }?.also { ktClass ->
                    (ktClass.companionObjects.firstOrNull()?.body?.properties
                        ?: ktClass.takeIf { it is KtObjectDeclaration }?.body?.properties)?.forEach { property ->
                        result[property.name ?: "Unknown name"] = property
                    }
                }
            }
            return result
        }
    }
}