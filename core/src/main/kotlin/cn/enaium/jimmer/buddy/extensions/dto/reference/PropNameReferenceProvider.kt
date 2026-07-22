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

package cn.enaium.jimmer.buddy.extensions.dto.reference

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiPropName
import cn.enaium.jimmer.buddy.utility.findCurrentImmutableType
import cn.enaium.jimmer.buddy.utility.findPropertyByName
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.KtClass

/**
 * @author Enaium
 */
object PropNameReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<out PsiReference> {
        return arrayOf(PropNameReference(element as? DtoPsiPropName ?: return emptyArray()))
    }

    class PropNameReference(
        private val propNameElement: DtoPsiPropName
    ) : PsiReferenceBase<DtoPsiPropName>(propNameElement, propNameElement.identifier.textRangeInParent) {

        override fun resolve(): PsiElement? {
            val value = propNameElement.text
            val currentImmutable = findCurrentImmutableType(propNameElement) ?: return null
            val prop = currentImmutable.props().find { it.name() == value } ?: return null

            JavaPsiFacade.getInstance(propNameElement.project)
                .findClass(currentImmutable.qualifiedName(), propNameElement.project.allScope())
                ?.also { klass ->
                    klass.allMethods.find { it.name == prop.name() }?.also { method ->
                        return method
                    }
                }

            KotlinFullClassNameIndex[currentImmutable.qualifiedName(), propNameElement.project, propNameElement.project.allScope()]
                .firstOrNull()
                ?.also { ktClass ->
                    (ktClass as KtClass).findPropertyByName(prop.name(), true)?.also {
                        return it
                    }
                }

            return null
        }

        override fun handleElementRename(newElementName: String): PsiElement {
            element.setName(newElementName)
            return element
        }
    }
}