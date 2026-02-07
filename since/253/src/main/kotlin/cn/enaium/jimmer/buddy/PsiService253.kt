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

package cn.enaium.jimmer.buddy

import cn.enaium.jimmer.buddy.service.PsiService
import cn.enaium.jimmer.buddy.utility.asKSClassDeclaration
import cn.enaium.jimmer.buddy.utility.createKSClassDeclaration
import cn.enaium.jimmer.buddy.utility.createKSName
import cn.enaium.jimmer.buddy.utility.createKSType
import com.intellij.openapi.application.ReadConstraint
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.idea.base.psi.typeArguments
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.*
import java.util.concurrent.Executor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author Enaium
 */
class PsiService253 : PsiService {
    override fun annotations(ktClass: KtClass): List<PsiService.Annotation> {
        return analyze(ktClass) {
            ktClass.symbol.annotations.map {
                PsiService.Annotation(
                    ktClass.project,
                    it.classId?.asFqNameString()?.replace("/", "."),
                    it.arguments.map { argument ->
                        PsiService.Annotation.Argument(
                            argument.name.asString(),
                            argument.expression.toAny(ktClass.project)
                        )
                    }
                )
            }
        }
    }

    override fun annotations(ktProperty: KtProperty): List<PsiService.Annotation> {
        return analyze(ktProperty) {
            ktProperty.symbol.annotations.map {
                PsiService.Annotation(
                    ktProperty.project,
                    it.classId?.asFqNameString()?.replace("/", "."),
                    it.arguments.map { argument ->
                        PsiService.Annotation.Argument(
                            argument.name.asString(),
                            argument.expression.toAny(ktProperty.project)
                        )
                    }
                )
            }
        }
    }

    @OptIn(KaExperimentalApi::class)
    override fun type(ktTypeReference: KtTypeReference): PsiService.Type {
        return analyze(ktTypeReference) {
            PsiService.Type(
                ktTypeReference.type.symbol?.classId?.asSingleFqName()?.asString()?.replace("/", "."),
                ktTypeReference.type.isMarkedNullable,
                ktTypeReference.type.symbol?.psi as? KtClass,
                (ktTypeReference).arguments().map {
                    type(it)
                }
            )
        }
    }

    @OptIn(KaExperimentalApi::class)
    override fun receiver(ktLambdaExpression: KtLambdaExpression): KtClass? {
        return analyze(ktLambdaExpression) {
            (ktLambdaExpression.expressionType as KaFunctionType).typeArguments.firstOrNull()?.type?.symbol?.psi as? KtClass
        }
    }

    fun KaAnnotationValue.toAny(project: Project): Any? {
        return when (this) {
            is KaAnnotationValue.ConstantValue -> this.value.toAny()
            is KaAnnotationValue.ArrayValue -> this.values.map { it.toAny(project) }
            is KaAnnotationValue.ClassLiteralValue -> this.classId?.asSingleFqName()?.asString()?.replace("/", ".")
                ?.let {
                    createKSType(
                        declaration = {
                            JavaPsiFacade.getInstance(project).findClass(it, project.allScope())?.asKSClassDeclaration()
                                ?: (KotlinFullClassNameIndex[it, project, project.allScope()].firstOrNull() as? KtClass)?.asKSClassDeclaration()
                                ?: createKSClassDeclaration(
                                    qualifiedName = {
                                        createKSName(it)
                                    },
                                    simpleName = {
                                        createKSName(it.substringAfterLast("."))
                                    },
                                    packageName = {
                                        createKSName(it.substringBeforeLast("."))
                                    },
                                    asType = {
                                        this@createKSType
                                    }
                                )
                        }
                    )
                }

            else -> {
                null
            }
        }
    }

    fun KaConstantValue.toAny(): Any? {
        return when (this) {
            is KaConstantValue.StringValue -> this.value
            is KaConstantValue.BooleanValue -> this.value
            is KaConstantValue.CharValue -> this.value
            is KaConstantValue.ByteValue -> this.value
            is KaConstantValue.DoubleValue -> this.value
            is KaConstantValue.ErrorValue -> this.value
            is KaConstantValue.FloatValue -> this.value
            is KaConstantValue.IntValue -> this.value
            is KaConstantValue.LongValue -> this.value
            is KaConstantValue.NullValue -> null
            is KaConstantValue.ShortValue -> this.value
            is KaConstantValue.UByteValue -> this.value
            is KaConstantValue.UIntValue -> this.value
            is KaConstantValue.ULongValue -> this.value
            is KaConstantValue.UShortValue -> this.value
        }
    }

    fun KtTypeReference.arguments(): List<KtTypeReference> {
        return (this.typeElement as? KtNullableType)?.typeArgumentsAsTypes ?: this.typeArguments()
            .mapNotNull { it.typeReference }
    }

    /**
     * Use [Write Allowing Read Action (WARA) API](https://plugins.jetbrains.com/docs/intellij/coroutine-read-actions.html#coroutine-read-actions-api)
     * instead of Write Blocking Read Action (WBRA) API
     */
    override suspend fun <T> readActionNonblockingCoroutine(
        project: Project,
        executor: Executor?,
        block: () -> T
    ): T {
        var context: CoroutineContext = EmptyCoroutineContext
        if (executor != null) {
            context += executor.asCoroutineDispatcher()
        }

        return withContext(context) {
            com.intellij.openapi.application.constrainedReadAction(
                ReadConstraint.withDocumentsCommitted(project),
                action = block
            )
        }
    }

    override suspend fun <T> readActionSmartNonblockingCoroutine(
        project: Project,
        executor: Executor?,
        block: () -> T
    ): T {
        var context: CoroutineContext = EmptyCoroutineContext
        if (executor != null) {
            context += executor.asCoroutineDispatcher()
        }

        return withContext(context) {
            com.intellij.openapi.application.constrainedReadAction(
                ReadConstraint.inSmartMode(project),
                ReadConstraint.withDocumentsCommitted(project),
                action = block
            )
        }
    }
}
