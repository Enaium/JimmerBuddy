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
import cn.enaium.jimmer.buddy.utility.*
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.idea.base.psi.typeArguments
import org.jetbrains.kotlin.idea.base.utils.fqname.fqName
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.constants.KClassValue.Value.NormalClass

/**
 * @author Enaium
 */
class PsiService242 : PsiService {
    override fun annotations(ktClass: KtClass): List<PsiService.Annotation> {
        if (isK2Enable()) {
            analyze(ktClass) {
                return ktClass.symbol.annotations.map {
                    PsiService.Annotation(
                        it.classId?.asFqNameString()?.replace("/", "."),
                        it.arguments.map { argument ->
                            PsiService.Annotation.Argument(argument.name.asString(), argument.expression.toAny())
                        }
                    )
                }
            }
        } else {
            return ktClass.annotationEntries.map {
                PsiService.Annotation(
                    it.annotation()?.fqName?.asString(),
                    it.annotation()?.allValueArguments?.map { (name, value) ->
                        PsiService.Annotation.Argument(
                            name.asString(),
                            value.toAny()
                        )
                    } ?: emptyList())
            }
        }
    }

    override fun annotations(ktProperty: KtProperty): List<PsiService.Annotation> {
        if (isK2Enable()) {
            analyze(ktProperty) {
                return ktProperty.symbol.annotations.map {
                    PsiService.Annotation(
                        it.classId?.asFqNameString()?.replace("/", "."),
                        it.arguments.map { argument ->
                            PsiService.Annotation.Argument(argument.name.asString(), argument.expression.toAny())
                        }
                    )
                }
            }
        } else {
            return ktProperty.annotationEntries.map {
                PsiService.Annotation(
                    it.annotation()?.fqName?.asString(),
                    it.annotation()?.allValueArguments?.map { (name, value) ->
                        PsiService.Annotation.Argument(
                            name.asString(),
                            value.toAny()
                        )
                    } ?: emptyList())
            }
        }
    }

    @OptIn(KaExperimentalApi::class)
    override fun type(ktTypeReference: KtTypeReference): PsiService.Type {
        if (isK2Enable()) {
            analyze(ktTypeReference) {
                return PsiService.Type(
                    ktTypeReference.type.symbol?.classId?.asSingleFqName()?.asString()?.replace("/", "."),
                    ktTypeReference.type.isMarkedNullable,
                    ktTypeReference.type.symbol?.psi as? KtClass,
                    (ktTypeReference).arguments().map {
                        type(it)
                    }
                )
            }
        } else {
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
    }

    @OptIn(KaExperimentalApi::class)
    override fun receiver(ktLambdaExpression: KtLambdaExpression): KtClass? {
        return if (isK2Enable()) {
            analyze(ktLambdaExpression) {
                (ktLambdaExpression.expressionType as KaFunctionType).typeArguments.firstOrNull()?.type?.symbol?.psi as? KtClass
            }
        } else {
            (ktLambdaExpression.analyze().get(
                BindingContext.EXPRESSION_TYPE_INFO,
                ktLambdaExpression
            )?.type?.arguments?.firstOrNull()?.type?.constructor?.declarationDescriptor as? ClassDescriptor)?.let {
                DescriptorToSourceUtils.getSourceFromDescriptor(
                    it
                ) as? KtClass
            }
        }
    }

    fun KtAnnotationEntry.annotation(): AnnotationDescriptor? =
        this.analyze()[BindingContext.ANNOTATION, this]

    fun ConstantValue<*>.toAny(): Any? {
        return when (this::class) {
            StringValue::class -> this.value.toString()
            BooleanValue::class -> this.value.toString().toBoolean()
            ArrayValue::class -> (this.value as? List<*>)?.map { (it as ConstantValue<*>).toAny() }
            TypedArrayValue::class -> (this.value as? List<*>)?.map { (it as ConstantValue<*>).toAny() }
            KClassValue::class -> (this.value as? NormalClass)?.classId?.asSingleFqName()?.asString()?.replace("/", ".")
                ?.let {
                    createKSType(
                        declaration = {
                            createKSClassDeclaration(
                                qualifiedName = {
                                    createKSName(it)
                                },
                                simpleName = {
                                    createKSName(it.substringAfterLast("."))
                                },
                                packageName = {
                                    createKSName(it.substringBeforeLast("."))
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

    fun KaAnnotationValue.toAny(): Any? {
        return when (this) {
            is KaAnnotationValue.ConstantValue -> this.value.toAny()
            is KaAnnotationValue.ArrayValue -> this.values.map { it.toAny() }
            is KaAnnotationValue.ClassLiteralValue -> this.classId?.asSingleFqName()?.asString()?.replace("/", ".")
                ?.let {
                    createKSType(
                        declaration = {
                            createKSClassDeclaration(
                                qualifiedName = {
                                    createKSName(it)
                                },
                                simpleName = {
                                    createKSName(it.substringAfterLast("."))
                                },
                                packageName = {
                                    createKSName(it.substringBeforeLast("."))
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
}