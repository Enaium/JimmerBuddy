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

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiLiteralExpression
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * @author Enaium
 */
class BuddyCompletionContributor : CompletionContributor() {
    private val basic = CompletionType.BASIC
    private val pattern =
        PlatformPatterns.or(
            PlatformPatterns.psiElement().withParent(PsiLiteralExpression::class.java)
                .inside(PsiAnnotation::class.java),
            PlatformPatterns.psiElement().withSuperParent(2, KtStringTemplateExpression::class.java)
                .inside(KtAnnotationEntry::class.java)
        )

    init {
        extend(basic, pattern, TableCompletionProvider)
        extend(basic, pattern, ColumnCompletionProvider)
    }
}