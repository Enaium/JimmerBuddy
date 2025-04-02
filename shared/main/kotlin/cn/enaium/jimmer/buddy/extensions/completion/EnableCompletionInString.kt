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

package cn.enaium.jimmer.buddy.extensions.completion

import cn.enaium.jimmer.buddy.utility.isImmutable
import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.codeInsight.completion.SkipAutopopupInStrings
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.sql.Entity
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.toUElementOfType

/**
 * @author Enaium
 */
class EnableCompletionInString : CompletionConfidence() {
    private val jimmerAnnotationPrefixes = listOf(
        Immutable::class.qualifiedName!!.substringBeforeLast("."),
        Entity::class.qualifiedName!!.substringBeforeLast(".")
    )

    override fun shouldSkipAutopopup(contextElement: PsiElement, psiFile: PsiFile, offset: Int): ThreeState {
        val inString = SkipAutopopupInStrings.isInStringLiteral(contextElement)
        val inImmutable = (psiFile.getChildOfType<PsiClass>()?.isImmutable() ?: psiFile.getChildOfType<KtClass>()
            ?.isImmutable()) == true
        val inJimmerAnnotation = jimmerAnnotationPrefixes.any {
            (contextElement.getParentOfType<PsiAnnotation>(true) ?: contextElement.getParentOfType<KtAnnotationEntry>(
                true
            )).toUElementOfType<UAnnotation>()?.qualifiedName?.startsWith(it) == true
        }

        return if (inString && inImmutable && inJimmerAnnotation) {
            ThreeState.NO
        } else {
            ThreeState.UNSURE
        }
    }
}