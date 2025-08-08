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

import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethodCallExpression
import org.babyfish.jimmer.sql.ast.query.selectable.RootSelectable
import org.babyfish.jimmer.sql.kt.ast.query.KRootSelectable
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

/**
 * @author Enaium
 */
class SelectSerialInlayHintsProvider : InlayHintsProvider {
    override fun createCollector(
        file: PsiFile,
        editor: Editor
    ): SharedBypassCollector {
        return object : SharedBypassCollector {
            override fun collectFromElement(
                element: PsiElement,
                sink: InlayTreeSink
            ) {
                if (element is PsiMethodCallExpression && element.resolveMethod()
                        ?.let { method -> method.name == "select" && method.containingClass?.qualifiedName == RootSelectable::class.qualifiedName } == true
                ) {
                    val expressions = element.argumentList.expressions
                    if (expressions.size > 1) {
                        expressions.forEachIndexed { idx, expression ->
                            val position = InlineInlayPosition(expression.textRange.endOffset, relatedToPrevious = true)
                            sink.addPresentation(position, hasBackground = true) {
                                text("${idx + 1}")
                            }
                        }
                    }
                }

                if (element is KtCallExpression && element.calleeExpression?.let { expression ->
                        expression.text == "select" && expression.reference?.resolve()
                            ?.let { function -> function is KtFunction && function.containingClass()?.fqName?.asString() == KRootSelectable::class.qualifiedName } == true
                    } == true) {
                    val expressions = element.valueArguments.mapNotNull { it.getChildOfType<KtQualifiedExpression>() }
                    if (expressions.size > 1) {
                        expressions.forEachIndexed { idx, expression ->
                            val position = InlineInlayPosition(expression.textRange.endOffset, relatedToPrevious = true)
                            sink.addPresentation(position, hasBackground = true) {
                                text("${idx + 1}")
                            }
                        }
                    }
                }
            }
        }
    }
}