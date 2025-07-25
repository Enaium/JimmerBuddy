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

import cn.enaium.jimmer.buddy.utility.annotName
import cn.enaium.jimmer.buddy.utility.jimmerAnnotationPrefixe
import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.codeInsight.completion.SkipAutopopupInStrings
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState

/**
 * @author Enaium
 */
class EnableCompletionInString : CompletionConfidence() {
    override fun shouldSkipAutopopup(contextElement: PsiElement, psiFile: PsiFile, offset: Int): ThreeState {
        val inString = SkipAutopopupInStrings.isInStringLiteral(contextElement)
        val inJimmerAnnotation = contextElement.annotName()?.startsWith(jimmerAnnotationPrefixe) == true

        return if (inString && inJimmerAnnotation) {
            ThreeState.NO
        } else {
            ThreeState.UNSURE
        }
    }
}