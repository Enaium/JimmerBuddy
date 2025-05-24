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

package cn.enaium.jimmer.buddy.extensions.insight

import cn.enaium.jimmer.buddy.utility.isImmutable
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import com.intellij.codeInsight.hints.codeVision.CodeVisionProviderBase
import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtClass

/**
 * @author Enaium
 */
abstract class ImmutableCodeVisionProvider : CodeVisionProviderBase() {
    override val relativeOrderings: List<CodeVisionRelativeOrdering>
        get() = listOf(
            CodeVisionRelativeOrdering.CodeVisionRelativeOrderingBefore("java.inheritors"),
            CodeVisionRelativeOrdering.CodeVisionRelativeOrderingBefore("java.references"),
            CodeVisionRelativeOrdering.CodeVisionRelativeOrderingBefore("vcs.code.vision"),
        )

    override fun acceptsElement(element: PsiElement): Boolean {
        return (element is PsiClass && element.isImmutable()) || (element is KtClass && element.isImmutable())
    }

    override fun acceptsFile(file: PsiFile): Boolean {
        return file.language == JavaLanguage.INSTANCE || file.language == KotlinLanguage.INSTANCE
    }
}