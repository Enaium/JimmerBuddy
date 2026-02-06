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
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.concurrency.await
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.base.utils.fqname.fqName
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.constants.KClassValue.Value.NormalClass
import java.util.concurrent.Executor

/**
 * @author Enaium
 */
class PsiService231 : PsiService {
    override fun annotations(ktClass: KtClass): List<PsiService.Annotation> {
        return ktClass.annotationEntries.map {
            PsiService.Annotation(
                ktClass.project,
                it.annotation()?.fqName?.asString(),
                it.annotation()?.allValueArguments?.map { (name, value) ->
                    PsiService.Annotation.Argument(
                        name.asString(),
                        value.toAny(ktClass.project)
                    )
                } ?: emptyList()
            )
        }
    }

    override fun annotations(ktProperty: KtProperty): List<PsiService.Annotation> {
        return ktProperty.annotationEntries.map {
            PsiService.Annotation(
                ktProperty.project,
                it.annotation()?.fqName?.asString(),
                it.annotation()?.allValueArguments?.map { (name, value) ->
                    PsiService.Annotation.Argument(
                        name.asString(),
                        value.toAny(ktProperty.project)
                    )
                } ?: emptyList())
        }
    }

    override fun type(ktTypeReference: KtTypeReference): PsiService.Type {
        return ktTypeReference.analyze()[BindingContext.TYPE, ktTypeReference]!!.let {
            PsiService.Type(
                it.fqName?.asString(),
                it.isMarkedNullable,
                (it.constructor.declarationDescriptor as? ClassDescriptor)?.let {
                    DescriptorToSourceUtils.getSourceFromDescriptor(
                        it
                    ) as? KtClass
                        ?: (ktTypeReference.typeElement as? KtUserType)?.referenceExpression?.mainReference?.resolve() as? KtClass
                },
                it.arguments.map { arg ->
                    PsiService.Type(
                        arg.type.fqName?.asString(),
                        arg.type.isMarkedNullable,
                        (arg.type.constructor.declarationDescriptor as? ClassDescriptor)?.let {
                            DescriptorToSourceUtils.getSourceFromDescriptor(
                                it
                            ) as? KtClass
                        },
                    )
                }
            )
        }
    }

    override fun receiver(ktLambdaExpression: KtLambdaExpression): KtClass? {
        return (ktLambdaExpression.analyze().get(
            BindingContext.EXPRESSION_TYPE_INFO,
            ktLambdaExpression
        )?.type?.arguments?.firstOrNull()?.type?.constructor?.declarationDescriptor as? ClassDescriptor)?.let {
            DescriptorToSourceUtils.getSourceFromDescriptor(
                it
            ) as? KtClass
        }
    }

    fun KtAnnotationEntry.annotation(): AnnotationDescriptor? =
        this.analyze()[BindingContext.ANNOTATION, this]

    fun ConstantValue<*>.toAny(project: Project): Any? {
        return when (this::class) {
            StringValue::class -> this.value.toString()
            BooleanValue::class -> this.value.toString().toBoolean()
            ArrayValue::class -> (this.value as? List<*>)?.map { (it as ConstantValue<*>).toAny(project) }
            TypedArrayValue::class -> (this.value as? List<*>)?.map { (it as ConstantValue<*>).toAny(project) }
            KClassValue::class -> (this.value as? NormalClass)?.classId?.asSingleFqName()?.asString()?.replace("/", ".")
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
                        },
                    )
                }

            else -> {
                null
            }
        }
    }

    override suspend fun <T> readActionNonblockingCoroutine(
        project: Project,
        executor: Executor?,
        block: () -> T
    ): T {
        return ReadAction.nonBlocking<T> { block() }
            .withDocumentsCommitted(project)
            .expireWith(project as Disposable)
            .submit(executor ?: AppExecutorUtil.getAppExecutorService())
            .await()
    }

    override suspend fun <T> readActionSmartNonblockingCoroutine(
        project: Project,
        executor: Executor?,
        block: () -> T
    ): T {
        return ReadAction.nonBlocking<T> { block() }
            .inSmartMode(project)
            .withDocumentsCommitted(project)
            .expireWith(project as Disposable)
            .submit(executor ?: AppExecutorUtil.getAppExecutorService())
            .await()
    }
}
