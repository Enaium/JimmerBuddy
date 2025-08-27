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

package cn.enaium.jimmer.buddy.extensions.inspection

import cn.enaium.jimmer.buddy.utility.*
import cn.enaium.jimmer.buddy.utility.CommonImmutableType.CommonImmutableProp.Companion.isAutoScalar
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import org.babyfish.jimmer.sql.JSqlClient
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

/**
 * @author Enaium
 */
class UnloadInspection : AbstractLocalInspectionTool() {
    override fun visit(
        element: PsiElement,
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ) {
        if (element is PsiMethodCallExpression && element.resolveMethod()?.containingClass?.isEntity() == true) {
            element.findExecuteMethod()?.also { execute ->
                execute.findFetcherExpression()?.also { fetcherExpression ->
                    val fetcher = fetcherExpression.getFetcher()
                    val trace = element.getImmutableTrace(execute)
                    if (!fetcher.contains(trace.joinToString("."))) {
                        var allScalarFields = false
                        val resolveMethod = element.resolveMethod() ?: return

                        if (resolveMethod.name == "id") {
                            return
                        }

                        if (fetcher.contains(trace.toMutableList().also {
                                it.removeLast()
                                it.add("allScalarFields")
                            }.joinToString("."))
                        ) {
                            if (resolveMethod.containingClass?.toImmutable()?.toCommonImmutableType()?.props()
                                    ?.find { it.name() == resolveMethod.name }?.isAutoScalar() == true
                            ) {
                                allScalarFields = true
                            }
                        }

                        if (!allScalarFields) {
                            element.firstChild?.lastChild?.also { name ->
                                holder.registerProblem(name, I18n.message("inspection.unloaded"))
                            }
                        }
                    }
                }
            }
        }
        if (element is KtQualifiedExpression && (element.lastChild.reference?.resolve() as? KtProperty)?.containingClass()
                ?.isEntity() == true
        ) {
            element.findExecuteFun()?.also { execute ->
                execute.findFetcherExpression()?.also { fetcherExpression ->
                    val fetcher = fetcherExpression.getFetcher()
                    val trace = element.getImmutableTrace(execute)
                    if (!fetcher.contains(trace.joinToString("."))) {
                        var allScalarFields = false
                        val property =
                            ((element.lastChild as? KtNameReferenceExpression)?.reference?.resolve() as? KtProperty)

                        if (property?.name == "id") {
                            return
                        }

                        if (fetcher.contains(trace.toMutableList().also {
                                it.removeLast()
                                it.add("allScalarFields")
                            }.joinToString("."))
                        ) {
                            if (property?.containingClass()?.toImmutable()?.toCommonImmutableType()?.props()
                                    ?.find { it.name() == property.name }?.isAutoScalar() == true
                            ) {
                                allScalarFields = true
                            }
                        }
                        if (!allScalarFields) {
                            element.lastChild?.firstChild?.also { name ->
                                holder.registerProblem(name, I18n.message("inspection.unloaded"))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun PsiMethodCallExpression.findFetcherExpression(): PsiMethodCallExpression? {

        var expression: PsiExpression? = null
        firstChild?.firstChild?.also { query ->
            if (query is PsiMethodCallExpression) {
                query.argumentList.expressions.forEach { fetchExpression ->
                    if (fetchExpression is PsiMethodCallExpression) {
                        val fetchMethod = fetchExpression.resolveMethod()
                        if (fetchMethod?.name == "fetch"
                            && fetchMethod.parameterList.parameters.size == 1
                            && fetchMethod.parameterList.parameters[0].type.resolveClass()?.qualifiedName == Fetcher::class.qualifiedName
                        ) {
                            expression = fetchExpression.argumentList.expressions[0]
                        }
                    }
                }
            }
        }

        val resolveMethod = resolveMethod()
        if (resolveMethod?.containingClass?.qualifiedName == JSqlClient::class.qualifiedName!!) {
            if (resolveMethod.parameterList.parameters.size > 1 && resolveMethod.parameterList.parameters[0].type.resolveClass()?.qualifiedName == Fetcher::class.qualifiedName) {
                expression = argumentList.expressions[0]
            }
        }

        if (expression != null) {
            if (expression is PsiReferenceExpression) {
                expression.reference?.resolve()?.also {
                    if (it is PsiField) {
                        return it.getChildOfType<PsiMethodCallExpression>()
                    }
                }
            } else if (expression is PsiMethodCallExpression) {
                return expression
            }
        }

        return null
    }

    private fun PsiMethodCallExpression.getFetcher(): Set<String> {
        val fetcher = mutableSetOf<String>()
        fun PsiMethodCallExpression.fetchResolver(parent: String = "") {
            var child: PsiElement? = this

            while (child != null) {
                if (child is PsiMethodCallExpression) {
                    val resolveMethod = child.resolveMethod()
                    fetcher.add("$parent${resolveMethod?.name}")
                    if (resolveMethod?.parameterList?.parameters?.size == 1 && resolveMethod.parameterList.parameters[0].type.resolveClass()?.qualifiedName == Fetcher::class.qualifiedName) {
                        val expression = child.argumentList.expressions[0]
                        if (expression is PsiMethodCallExpression) {
                            expression.fetchResolver("$parent${resolveMethod.name}.")
                        }
                    }
                }
                child = child.firstChild?.firstChild
            }
        }
        fetchResolver()
        return fetcher
    }

    private fun KtQualifiedExpression.findFetcherExpression(): KtQualifiedExpression? {
        var expression: KtExpression? = null
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
            query.lambdaArguments.lastOrNull()?.getLambdaExpression()?.bodyExpression
                ?.getChildrenOfType<KtCallExpression>()?.lastOrNull()?.also { select ->
                    select.valueArguments.firstOrNull()?.also { tableArg ->
                        val tableFetchQE = tableArg.getChildOfType<KtQualifiedExpression>()
                        tableFetchQE?.getChildOfType<KtCallExpression>()?.also { fetch ->
                            fetch.takeIf { it.firstChild.text == "fetch" }?.valueArguments?.firstOrNull()
                                ?.also { fetchArg ->
                                    fetchArg.getChildOfType<KtQualifiedExpression>()
                                        ?.also { fetcherExpression ->
                                            expression = fetcherExpression
                                        }
                                    fetchArg.getChildOfType<KtNameReferenceExpression>()
                                        ?.also { fetcherExpression ->
                                            expression = fetcherExpression
                                        }
                                }
                            fetch.takeIf { it.firstChild.text == "fetchBy" }?.also { fetchBy ->
                                expression = tableFetchQE
                            }
                        }
                    }
                }

            if (expression == null) {
                val ktFun = query.firstChild.reference?.resolve() as? KtNamedFunction
                if (query.valueArguments.size > 1 && ktFun?.valueParameters[0]?.typeReference?.type()?.fqName == Fetcher::class.qualifiedName) {
                    expression = query.valueArguments[0].firstChild as? KtExpression
                }
            }
        }

        if (expression != null) {
            if (expression is KtQualifiedExpression) {
                return expression
            } else if (expression is KtNameReferenceExpression) {
                return (expression.reference?.resolve() as? KtProperty)?.getChildOfType<KtQualifiedExpression>()
            }
        }

        return null
    }

    private fun KtQualifiedExpression.getFetcher(): Set<String> {
        val fetcher = mutableSetOf<String>()
        fun KtCallExpression.fetchResolver(parent: String = "") {
            lambdaArguments[0].getLambdaExpression()?.functionLiteral?.bodyBlockExpression?.getChildrenOfType<KtCallExpression>()
                ?.forEach { expression ->
                    fetcher.add("$parent${expression.firstChild.text}")
                    if (expression.lambdaArguments.isNotEmpty()) {
                        expression.fetchResolver(parent = "$parent${expression.firstChild.text}.")
                    }
                }
        }
        if (lastChild is KtCallExpression) {
            (lastChild as KtCallExpression).fetchResolver()
        }
        return fetcher
    }
}