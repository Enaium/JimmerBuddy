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
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.BooleanValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.idea.base.utils.fqname.fqName
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.TypedArrayValue

/**
 * @author Enaium
 */
class PsiShared232 : PsiShared {
    override fun annotations(ktClass: KtClass): List<PsiShared.Annotation> {
        return ktClass.annotationEntries.map {
            PsiShared.Annotation(
                it.annotation()?.fqName!!.asString(),
                it.annotation()?.allValueArguments?.map { (name, value) ->
                    PsiShared.Annotation.Argument(
                        name.asString(),
                        value.toAny()
                    )
                } ?: emptyList()
            )
        }
    }

    override fun annotations(ktProperty: KtProperty): List<PsiShared.Annotation> {
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

    override fun type(ktTypeReference: KtTypeReference): PsiShared.Type {
        return ktTypeReference.analyze()[BindingContext.TYPE, ktTypeReference]!!.let {
            PsiShared.Type(
                it.fqName!!.asString(),
                it.isMarkedNullable,
                (it.constructor.declarationDescriptor as? ClassDescriptor)?.let {
                    DescriptorToSourceUtils.getSourceFromDescriptor(
                        it
                    ) as? KtClass
                },
                it.arguments.map { arg ->
                    PsiShared.Type(
                        arg.type.fqName!!.asString(),
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
}