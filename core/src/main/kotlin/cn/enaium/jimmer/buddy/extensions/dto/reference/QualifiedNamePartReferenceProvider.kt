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

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiQualifiedName
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiQualifiedNamePart
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.KtClass

/**
 * @author Enaium
 */
object QualifiedNamePartReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<out PsiReference> {
        return arrayOf(QualifiedNamePartReference(element as DtoPsiQualifiedNamePart))
    }

    class QualifiedNamePartReference(
        private val qualifiedNamePart: DtoPsiQualifiedNamePart
    ) : PsiReferenceBase<DtoPsiQualifiedNamePart>(
        qualifiedNamePart,
        qualifiedNamePart.identifier.textRangeInParent
    ) {
        override fun resolve(): PsiElement? {
            val qualifiedName = PsiTreeUtil.getParentOfType(qualifiedNamePart, DtoPsiQualifiedName::class.java)
                ?: return null

            val parts = qualifiedName.qualifiedNamePartList.map { it.identifier.text }
            val partIndex = qualifiedName.qualifiedNamePartList.indexOf(qualifiedNamePart)
            val isLastPart = partIndex == qualifiedName.qualifiedNamePartList.size - 1

            if (isLastPart) {
                // Last part: resolve as type/class
                val fullQualifiedName = parts.joinToString(".")
                return resolveClass(fullQualifiedName)
            } else {
                // Not last part: resolve as package
                val packageName = parts.take(partIndex + 1).joinToString(".")
                return JavaPsiFacade.getInstance(qualifiedNamePart.project).findPackage(packageName)
            }
        }

        override fun getVariants(): Array<Any> = emptyArray()

        private fun resolveClass(qualifiedName: String): PsiElement? {
            JavaPsiFacade.getInstance(qualifiedNamePart.project)
                .findClass(qualifiedName, qualifiedNamePart.project.allScope())
                ?.let { return it }

            KotlinFullClassNameIndex[qualifiedName, qualifiedNamePart.project, qualifiedNamePart.project.allScope()]
                .firstOrNull()
                ?.let { ktClass ->
                    return ktClass as KtClass
                }

            return null
        }
    }
}