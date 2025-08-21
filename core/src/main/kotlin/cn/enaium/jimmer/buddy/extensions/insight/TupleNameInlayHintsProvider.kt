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

import cn.enaium.jimmer.buddy.utility.findExecuteFun
import cn.enaium.jimmer.buddy.utility.findExecuteMethod
import com.intellij.codeInsight.hints.declarative.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import org.babyfish.jimmer.sql.ast.tuple.Tuple2
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

/**
 * @author Enaium
 */
class TupleNameInlayHintsProvider : InlayHintsProvider {
    override fun createCollector(
        file: PsiFile,
        editor: Editor
    ): SharedBypassCollector {
        return object : SharedBypassCollector {
            override fun collectFromElement(
                element: PsiElement,
                sink: InlayTreeSink
            ) {
                if (element is PsiMethodCallExpression) {
                    val methodExpression = element.methodExpression
                    val reference = methodExpression.reference?.resolve()
                    if (reference is PsiMethod && reference.containingClass?.qualifiedName?.startsWith(Tuple2::class.java.packageName) == true) {
                        element.findExecuteMethod()?.also { execute ->
                            val selectExpressions = execute.findSelectExpressions()
                            val map = selectExpressions
                                .mapIndexed { idx, expression -> "get_${idx + 1}" to expression }.toMap()
                            val position =
                                InlineInlayPosition(methodExpression.textRange.endOffset, relatedToPrevious = true)
                            sink.addPresentation(position, hasBackground = true) {
                                val expression =
                                    map[methodExpression.referenceNameElement?.text] ?: return@addPresentation
                                text(
                                    "${expression.methodExpression.referenceNameElement?.text}", InlayActionData(
                                        PsiPointerInlayActionPayload(expression.createSmartPointer()),
                                        PsiPointerInlayActionNavigationHandler.HANDLER_ID
                                    )
                                )
                            }
                        }
                    }
                } else if (element is KtQualifiedExpression) {
                    val selectorExpression = element.selectorExpression
                    val reference = selectorExpression?.reference?.resolve()
                    if (reference is KtProperty && reference.containingClass()?.fqName?.asString()
                            ?.startsWith(Tuple2::class.java.packageName) == true
                    ) {
                        element.findExecuteFun()?.also { execute ->
                            val selectExpressions = execute.findSelectExpressions()
                            val map = selectExpressions
                                .mapIndexed { idx, expression -> "_${idx + 1}" to expression }.toMap()
                            val position =
                                InlineInlayPosition(selectorExpression.textRange.endOffset, relatedToPrevious = true)
                            sink.addPresentation(position, hasBackground = true) {
                                val expression = map[selectorExpression.text] ?: return@addPresentation
                                text(
                                    "${expression.tupleName()}", InlayActionData(
                                        PsiPointerInlayActionPayload(expression.createSmartPointer()),
                                        PsiPointerInlayActionNavigationHandler.HANDLER_ID
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun KtQualifiedExpression.tupleName(): String? {
        return if (selectorExpression is KtCallExpression) {
            when (receiverExpression) {
                is KtQualifiedExpression -> {
                    (receiverExpression as? KtQualifiedExpression)?.selectorExpression?.text
                }

                is KtCallExpression -> {
                    (receiverExpression as? KtCallExpression)?.valueArguments?.firstOrNull()
                        ?.getChildOfType<KtQualifiedExpression>()?.tupleName()
                }

                else -> {
                    selectorExpression?.text
                }
            }
        } else {
            selectorExpression?.text
        }
    }

    private fun PsiMethodCallExpression.findSelectExpressions(): List<PsiMethodCallExpression> {
        firstChild?.firstChild?.also { query ->
            if (query is PsiMethodCallExpression) {
                return query.argumentList.expressions.filterIsInstance<PsiMethodCallExpression>().toList()
            }
        }

        return emptyList()
    }

    private fun KtQualifiedExpression.findSelectExpressions(): List<KtQualifiedExpression> {
        var query: KtCallExpression? = null

        firstChild?.lastChild?.also {
            if (it is KtCallExpression) {
                query = it
            }
        }

        if (query == null) {
            firstChild?.also {
                if (it is KtCallExpression) {
                    query = it
                }
            }
        }

        if (query == null) {
            lastChild?.also {
                if (it is KtCallExpression) {
                    query = it
                }
            }
        }

        if (query is KtCallExpression) {
            query.lambdaArguments[0].getLambdaExpression()?.bodyExpression?.getChildrenOfType<KtCallExpression>()
                ?.lastOrNull()?.also { select ->
                    return select.valueArguments.mapNotNull { it.getChildOfType<KtQualifiedExpression>() }
                }
        }
        return emptyList()
    }
}