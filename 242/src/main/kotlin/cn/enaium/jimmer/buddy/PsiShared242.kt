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

import cn.enaium.jimmer.buddy.utility.PsiShared
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
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

/**
 * @author Enaium
 */
class PsiShared242 : PsiShared {
    private fun isK2Enable(): Boolean {
        return System.getProperty("idea.kotlin.plugin.use.k2")?.toBoolean() == true
    }

    override fun annotations(ktClass: KtClass): List<PsiShared.Annotation> {
        if (isK2Enable()) {
            analyze(ktClass) {
                return ktClass.symbol.annotations.map {
                    PsiShared.Annotation(
                        it.classId?.asFqNameString()!!.replace("/", "."),
                        it.arguments.map { argument ->
                            PsiShared.Annotation.Argument(argument.name.asString(), argument.expression.toAny())
                        }
                    )
                }
            }
        } else {
            return ktClass.annotationEntries.map {
                PsiShared.Annotation(
                    it.annotation()?.fqName!!.asString(),
                    it.annotation()?.allValueArguments?.map { (name, value) ->
                        PsiShared.Annotation.Argument(
                            name.asString(),
                            value.toAny()
                        )
                    } ?: emptyList())
            }
        }
    }

    override fun annotations(ktProperty: KtProperty): List<PsiShared.Annotation> {
        if (isK2Enable()) {
            analyze(ktProperty) {
                return ktProperty.symbol.annotations.map {
                    PsiShared.Annotation(
                        it.classId?.asFqNameString()!!.replace("/", "."),
                        it.arguments.map { argument ->
                            PsiShared.Annotation.Argument(argument.name.asString(), argument.expression.toAny())
                        }
                    )
                }
            }
        } else {
            return ktProperty.annotationEntries.map {
                PsiShared.Annotation(
                    it.annotation()?.fqName!!.asString(),
                    it.annotation()?.allValueArguments?.map { (name, value) ->
                        PsiShared.Annotation.Argument(
                            name.asString(),
                            value.toAny()
                        )
                    } ?: emptyList())
            }
        }
    }

    @OptIn(KaExperimentalApi::class)
    override fun type(ktTypeReference: KtTypeReference): PsiShared.Type {
        if (isK2Enable()) {
            analyze(ktTypeReference) {
                return PsiShared.Type(
                    ktTypeReference.type.symbol?.classId?.asFqNameString()!!.replace("/", "."),
                    ktTypeReference.type.isMarkedNullable,
                    ktTypeReference.type.symbol?.psi as? KtClass,
                    (ktTypeReference).arguments().map {
                        type(it)
                    }
                )
            }
        } else {
            return ktTypeReference.analyze()[BindingContext.TYPE, ktTypeReference]!!.let {
                PsiShared.Type(
                    it.fqName!!.asString(),
                    it.isMarkedNullable,
                    (it.constructor.declarationDescriptor as? ClassDescriptor)?.let {
                        DescriptorToSourceUtils.getSourceFromDescriptor(
                            it
                        ) as? KtClass
                            ?: (ktTypeReference.typeElement as? KtUserType)?.referenceExpression?.mainReference?.resolve() as? KtClass
                    },
                    it.arguments.map { arg ->
                        PsiShared.Type(
                            arg.type.fqName!!.asString(),
                            arg.type.isMarkedNullable,
                            (arg.type.constructor.declarationDescriptor as? ClassDescriptor)?.let {
                                DescriptorToSourceUtils.getSourceFromDescriptor(
                                    it
                                ) as? KtClass
                                    ?: (ktTypeReference.typeElement as? KtUserType)?.referenceExpression?.mainReference?.resolve() as? KtClass
                            },
                        )
                    }
                )
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
            else -> {
                null
            }
        }
    }

    fun KaAnnotationValue.toAny(): Any? {
        return when (this) {
            is KaAnnotationValue.ConstantValue -> this.value.toAny()
            is KaAnnotationValue.ArrayValue -> this.values.map { it.toAny() }
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