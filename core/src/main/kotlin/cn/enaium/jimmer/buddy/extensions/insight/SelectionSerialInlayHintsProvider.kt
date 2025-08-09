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
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import org.babyfish.jimmer.sql.ast.query.MutableBaseQuery
import org.babyfish.jimmer.sql.ast.query.TypedBaseQuery
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableBaseQuery
import org.babyfish.jimmer.sql.kt.ast.query.KMutableBaseQuery
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

/**
 * @author Enaium
 */
class SelectionSerialInlayHintsProvider : InlayHintsProvider {
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
                        ?.let {
                            it.name in listOf(
                                "asBaseTable",
                                "asCteBaseTable"
                            ) && it.containingClass?.qualifiedName == TypedBaseQuery::class.qualifiedName
                        } == true
                ) {
                    val expressions = element.findSelectionAddExpressions()
                    if (expressions.size > 1) {
                        expressions.forEachIndexed { idx, expression ->
                            val position =
                                InlineInlayPosition(expression.textRange.endOffset, relatedToPrevious = true)
                            sink.addPresentation(position, hasBackground = true) {
                                text("${idx + 1}")
                            }
                        }
                    }
                } else if (element is KtBlockExpression) {
                    element.getChildrenOfType<KtQualifiedExpression>().lastOrNull()?.also { selection ->
                        val expressions = selection.findSelectionAddExpressions()
                        if (expressions.size > 1) {
                            expressions.forEachIndexed { idx, expression ->
                                val position =
                                    InlineInlayPosition(expression.textRange.endOffset, relatedToPrevious = true)
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

    fun PsiMethodCallExpression.findSelectionAddExpressions(): List<PsiMethodCallExpression> {
        val expressions = mutableListOf<PsiMethodCallExpression>()
        var child: PsiMethodCallExpression? = this
        while (child != null) {
            if (child.methodExpression.let { expression ->
                    expression.referenceNameElement?.text == "addSelect" && expression.reference?.resolve()
                        ?.let { method ->
                            method is PsiMethod && listOfNotNull(
                                MutableBaseQuery::class.java.packageName,
                                KConfigurableBaseQuery.Query1::class.java.packageName
                            ).any {
                                method.containingClass?.qualifiedName?.startsWith(it) == true
                            }
                        } == true
                }) {
                expressions.add(child)
            }

            child = child.methodExpression.qualifierExpression as? PsiMethodCallExpression
        }
        return expressions.reversed()
    }

    fun KtQualifiedExpression.findSelectionAddExpressions(): List<KtQualifiedExpression> {
        val expressions = mutableListOf<KtQualifiedExpression>()
        var child: KtQualifiedExpression? = this
        while (child != null) {

            if ((child.selectorExpression as? KtCallExpression)?.calleeExpression?.let { expression ->
                    expression.text == "add" && expression.reference?.resolve()
                        ?.let { function ->
                            function is KtFunction && listOfNotNull(
                                KMutableBaseQuery.Selections::class.java.packageName,
                                KConfigurableBaseQuery.Query1::class.java.packageName
                            ).any { function.containingClass()?.fqName?.asString()?.startsWith(it) == true }
                        } == true
                } == true) {
                expressions.add(child)
            }

            child = child.receiverExpression as? KtQualifiedExpression
        }
        return expressions.reversed()
    }
}