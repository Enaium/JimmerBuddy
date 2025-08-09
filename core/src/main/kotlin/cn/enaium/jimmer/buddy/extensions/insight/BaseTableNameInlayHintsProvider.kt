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

import com.intellij.codeInsight.hints.declarative.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.util.findParentOfType
import org.babyfish.jimmer.sql.ast.table.base.BaseTable1
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.table.KNonNullBaseTable1
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*

/**
 * @author Enaium
 */
class BaseTableNameInlayHintsProvider : InlayHintsProvider {
    override fun createCollector(
        file: PsiFile,
        editor: Editor
    ): InlayHintsCollector {
        return object : SharedBypassCollector {
            override fun collectFromElement(
                element: PsiElement,
                sink: InlayTreeSink
            ) {
                if (element is PsiMethodCallExpression) {
                    val methodExpression = element.methodExpression
                    val reference = methodExpression.reference?.resolve()
                    if (reference is PsiMethod && reference.containingClass?.qualifiedName
                            ?.startsWith(BaseTable1::class.java.packageName) == true
                    ) {
                        element.findBaseQueryExpression()?.also { baseQuery ->
                            val addArgumentExpressions = baseQuery.findAddArgumentExpressions()
                            val map = addArgumentExpressions
                                .mapIndexed { idx, expression -> "get_${idx + 1}" to expression }.toMap()
                            val position =
                                InlineInlayPosition(methodExpression.textRange.endOffset, relatedToPrevious = true)
                            sink.addPresentation(position, hasBackground = true) {
                                val expression =
                                    map[methodExpression.referenceNameElement?.text] ?: return@addPresentation
                                val text = when (expression) {
                                    is PsiMethodCallExpression -> expression.methodExpression.referenceNameElement?.text
                                    is PsiReferenceExpression -> expression.text
                                    else -> null
                                }
                                text(
                                    text ?: "unknow", InlayActionData(
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
                            ?.startsWith(KNonNullBaseTable1::class.java.packageName) == true
                    ) {
                        element.findBaseQueryExpression()?.also { baseQuery ->
                            val addArgumentExpressions = baseQuery.findAddArgumentExpressions()
                            val map =
                                addArgumentExpressions.mapIndexed { idx, expression -> "_${idx + 1}" to expression }
                                    .toMap()

                            val position =
                                InlineInlayPosition(element.textRange.endOffset, relatedToPrevious = true)
                            sink.addPresentation(position, hasBackground = true) {
                                val expression = map[selectorExpression.text] ?: return@addPresentation
                                val text = when (expression) {
                                    is KtQualifiedExpression -> expression.selectorExpression?.text
                                    is KtNameReferenceExpression -> expression.text
                                    else -> null
                                }
                                text(
                                    text ?: "unknow", InlayActionData(
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

    fun PsiMethodCallExpression.findBaseQueryExpression(): PsiMethodCallExpression? {
        val reference = methodExpression.qualifierExpression?.reference?.resolve()
        if (reference is PsiLocalVariable) {
            return reference.initializer as? PsiMethodCallExpression
        }
        return null
    }


    fun PsiMethodCallExpression.findAddArgumentExpressions(): List<PsiExpression> {
        val expressions = mutableListOf<PsiExpression>()
        var child: PsiMethodCallExpression? = this
        while (child != null) {
            if (child.methodExpression.referenceNameElement?.text == "addSelect") {
                if (child.argumentList.expressions.size == 1) {
                    expressions.add(child.argumentList.expressions[0])
                }
            }
            child = child.methodExpression.qualifierExpression as? PsiMethodCallExpression
        }
        return expressions.reversed()
    }

    fun KtQualifiedExpression.findBaseQueryExpression(): KtQualifiedExpression? {
        var parent = getParentOfType<KtQualifiedExpression>(true)
        while (parent != null) {
            val selectorExpression = parent.selectorExpression as? KtCallExpression
            if (selectorExpression?.valueArguments?.size == 2 && selectorExpression.lambdaArguments.size == 1) {
                val reference = selectorExpression.calleeExpression?.reference?.resolve()
                if (reference is KtFunction && reference.containingClass()?.fqName?.asString() == KSqlClient::class.qualifiedName) {
                    val callExpression =
                        when (val argumentExpression = selectorExpression.valueArguments[0].getArgumentExpression()) {
                            is KtNameReferenceExpression -> {
                                (argumentExpression.reference?.resolve() as? KtProperty)?.initializer as? KtCallExpression
                            }

                            is KtCallExpression -> {
                                argumentExpression
                            }

                            else -> null
                        }
                    if (callExpression?.calleeExpression?.reference?.resolve()
                            ?.let {
                                it is KtFunction && it.name in listOf(
                                    "baseTableSymbol",
                                    "cteBaseTableSymbol"
                                )
                            } == true
                    ) {
                        return callExpression.lambdaArguments.lastOrNull()
                            ?.getLambdaExpression()?.bodyExpression?.firstStatement as KtQualifiedExpression?
                    }

                    if (callExpression == null) {
                        when (val argumentExpression = selectorExpression.valueArguments[0].getArgumentExpression()) {
                            is KtNameReferenceExpression -> {
                                return ((argumentExpression.reference?.resolve() as? KtProperty)?.initializer as? KtQualifiedExpression)?.receiverExpression as? KtQualifiedExpression
                            }

                            is KtQualifiedExpression -> {
                                return argumentExpression.receiverExpression as? KtQualifiedExpression
                            }
                        }
                    }
                }
            }
            parent = parent.findParentOfType<KtQualifiedExpression>(true)
        }
        return null
    }

    fun KtQualifiedExpression.findAddArgumentExpressions(): List<KtExpression> {
        val expression = selectorExpression as? KtCallExpression ?: return emptyList()
        expression.lambdaArguments[0].getLambdaExpression()?.bodyExpression?.getChildrenOfType<KtQualifiedExpression>()
            ?.lastOrNull()?.also { selections ->
                val expressions = mutableListOf<KtExpression>()
                var child: KtQualifiedExpression? = selections
                while (child != null) {
                    val expression = child.selectorExpression as? KtCallExpression
                    if (expression?.valueArguments?.size == 1) {
                        expression.valueArguments[0].getArgumentExpression()?.also { expressions.add(it) }
                    }
                    child = child.getChildOfType<KtQualifiedExpression>()
                }
                return expressions.reversed()
            }
        return emptyList()
    }
}