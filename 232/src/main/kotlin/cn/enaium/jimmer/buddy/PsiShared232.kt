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
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.BindingContext

/**
 * @author Enaium
 */
class PsiShared232 : PsiShared {
    override fun annotations(ktClass: KtClass): List<PsiShared.Annotation> {
        return ktClass.annotationEntries.map { PsiShared.Annotation(it.annotation()?.fqName!!.asString()) }
    }

    override fun annotations(ktProperty: KtProperty): List<PsiShared.Annotation> {
        return ktProperty.annotationEntries.map { PsiShared.Annotation(it.annotation()?.fqName!!.asString()) }
    }

    override fun type(ktTypeReference: KtTypeReference): PsiShared.Type {
        return ktTypeReference.analyze()[BindingContext.TYPE, ktTypeReference]!!.let {
            PsiShared.Type(
                it.toString(),
                it.isMarkedNullable
            )
        }
    }

    fun KtAnnotationEntry.annotation(): AnnotationDescriptor? =
        this.analyze()[BindingContext.ANNOTATION, this]
}