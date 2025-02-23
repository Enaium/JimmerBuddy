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

package cn.enaium.jimmer.buddy.utility

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType

/**
 * @author Enaium
 */
fun KtAnnotationEntry.annotation(): AnnotationDescriptor? =
    this.analyze()[BindingContext.ANNOTATION, this]

fun KtClass.annotations(): List<AnnotationDescriptor?> =
    this.annotationEntries.map { it.annotation() }

fun KtProperty.annotations(): List<AnnotationDescriptor?> =
    this.annotationEntries.map { it.annotation() }

fun KtTypeReference.type(): KotlinType? = this.analyze()[BindingContext.TYPE, this]