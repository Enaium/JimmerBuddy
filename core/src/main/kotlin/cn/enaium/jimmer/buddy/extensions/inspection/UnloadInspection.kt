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
import org.babyfish.jimmer.sql.ast.Executable
import org.babyfish.jimmer.sql.ast.query.TypedRootQuery
import org.babyfish.jimmer.sql.fetcher.Fetcher
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.KExecutable
import org.babyfish.jimmer.sql.kt.ast.query.KTypedRootQuery
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
                    val trace = element.getTrace(execute)
                    if (!fetcher.contains(trace.joinToString("."))) {
                        var allScalarFields = false
                        if (fetcher.contains(trace.toMutableList().also {
                                it.removeLast()
                                it.add("allScalarFields")
                            }.joinToString("."))
                        ) {
                            val resolveMethod = element.resolveMethod() ?: return
                            if (resolveMethod.containingClass?.toImmutable()?.toCommonImmutableType()?.props()
                                    ?.find { it.name() == resolveMethod.name }?.isAutoScalar() == true
                            ) {
                                allScalarFields = true
                            }
                        }

                        if (!allScalarFields) {
                            element.firstChild?.lastChild?.also { name ->
                                holder.registerProblem(name, "The prop is unload")
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
                    val trace = element.getTrace(execute)
                    if (!fetcher.contains(trace.joinToString("."))) {
                        var allScalarFields = false
                        if (fetcher.contains(trace.toMutableList().also {
                                it.removeLast()
                                it.add("allScalarFields")
                            }.joinToString("."))
                        ) {
                            val property =
                                ((element.lastChild as? KtNameReferenceExpression)?.reference?.resolve() as? KtProperty)
                            if (property?.containingClass()?.toImmutable()?.toCommonImmutableType()?.props()
                                    ?.find { it.name() == property.name }?.isAutoScalar() == true
                            ) {
                                allScalarFields = true
                            }
                        }
                        if (!allScalarFields) {
                            element.lastChild?.firstChild?.also { name ->
                                holder.registerProblem(name, "The prop is unload")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun PsiMethodCallExpression.findExecuteMethod(): PsiMethodCallExpression? {
        var returnClass = type?.resolveClass()
        if (returnClass?.typeParameters?.size == 1) {
            returnClass = type?.resolveGenericsClass(returnClass.typeParameters[0] ?: return null)
        }

        val containingClass = resolveMethod()?.containingClass
        return if (returnClass?.isEntity() == true &&
            listOf(
                TypedRootQuery::class.qualifiedName,
                Executable::class.qualifiedName,
                JSqlClient::class.qualifiedName
            ).any { it == containingClass?.qualifiedName }
        ) {
            this
        } else {
            val child = firstChild?.firstChild
            if (child is PsiMethodCallExpression) {
                child.findExecuteMethod()
            } else if (child is PsiReferenceExpression) {
                val resolve = child.resolve()
                if (resolve is PsiLocalVariable) {
                    resolve.getChildOfType<PsiMethodCallExpression>()?.findExecuteMethod()
                } else {
                    null
                }
            } else {
                null
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

    private fun PsiMethodCallExpression.getTrace(execute: PsiMethodCallExpression): List<String> {
        val trace = mutableListOf<String>()

        var child: PsiElement? = this

        while (child != null) {
            if (child is PsiMethodCallExpression) {
                val resolveMethod = child.resolveMethod()
                if (resolveMethod?.containingClass?.isEntity() == true) {
                    trace.add(resolveMethod.name)
                } else if (child == execute) {
                    return trace.reversed()
                }
            }
            child = child.firstChild?.firstChild
        }

        return trace.reversed()
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

    private fun KtQualifiedExpression.findExecuteFun(): KtQualifiedExpression? {
        val callExpression = lastChild as? KtCallExpression
        return (if (callExpression != null && listOf(
                KTypedRootQuery::class.qualifiedName,
                KExecutable::class.qualifiedName,
                KSqlClient::class.qualifiedName
            ).any { it == (callExpression.firstChild?.reference?.resolve() as? KtNamedFunction)?.containingClass()?.fqName?.asString() }
        ) {
            this
        } else if (firstChild is KtQualifiedExpression) {
            (firstChild as KtQualifiedExpression).findExecuteFun()
        } else if (firstChild is KtArrayAccessExpression) {
            (firstChild.firstChild as KtQualifiedExpression).findExecuteFun()
        } else if (firstChild is KtNameReferenceExpression) {
            val resolve = firstChild.reference?.resolve()
            if (resolve is KtProperty) {
                resolve.getChildOfType<KtQualifiedExpression>()?.findExecuteFun()
            } else {
                null
            }
        } else {
            null
        })
    }

    private fun KtQualifiedExpression.findFetcherExpression(): KtQualifiedExpression? {
        var expression: KtExpression? = null

        firstChild?.lastChild?.also { query ->
            if (query is KtCallExpression) {
                query.lambdaArguments[0].getLambdaExpression()?.functionLiteral?.bodyBlockExpression
                    ?.getChildrenOfType<KtCallExpression>()?.lastOrNull()?.also { select ->
                        select.valueArguments[0].also { fetch ->
                            fetch.getChildOfType<KtQualifiedExpression>()
                                ?.getChildOfType<KtCallExpression>()
                                ?.takeIf { it.firstChild.text == "fetch" }?.valueArguments[0]?.getChildOfType<KtQualifiedExpression>()
                                ?.also { fetcherExpression ->
                                    expression = fetcherExpression
                                }
                            fetch.getChildOfType<KtQualifiedExpression>()
                                ?.getChildOfType<KtCallExpression>()
                                ?.takeIf { it.firstChild.text == "fetch" }?.valueArguments[0]?.getChildOfType<KtNameReferenceExpression>()
                                ?.also { fetcherExpression ->
                                    expression = fetcherExpression
                                }
                        }
                    }
            }
        }

        lastChild?.also { query ->
            if (query is KtCallExpression) {
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

    private fun KtQualifiedExpression.getTrace(execute: KtQualifiedExpression): List<String> {
        val trace = mutableListOf<String>()

        var child: PsiElement? = this

        while (child != null) {
            if (child is KtQualifiedExpression) {
                val property = child.lastChild.reference?.resolve() as? KtProperty
                if (property?.containingClass()?.isEntity() == true) {
                    trace.add(property.name ?: continue)
                } else if (child == execute) {
                    return trace.reversed()
                }
            }
            child = child.firstChild
            if (child is KtArrayAccessExpression) {
                child.firstChild
            }
        }

        return trace.reversed()
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