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

import cn.enaium.jimmer.buddy.JimmerBuddy.PSI_SHARED
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.util.PsiUtil
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.error.ErrorFamily
import org.babyfish.jimmer.sql.*
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty

/**
 * @author Enaium
 */
fun PsiClass.hasImmutableAnnotation(): Boolean {
    return this.modifierList?.annotations?.any { annotation ->
        annotation.hasQualifiedName(Immutable::class.qualifiedName!!)
                || annotation.hasQualifiedName(Entity::class.qualifiedName!!)
                || annotation.hasQualifiedName(MappedSuperclass::class.qualifiedName!!)
                || annotation.hasQualifiedName(Embeddable::class.qualifiedName!!)
    } == true
}

fun PsiClass.hasErrorFamilyAnnotation(): Boolean {
    return this.modifierList?.annotations?.any { annotation ->
        annotation.hasQualifiedName(ErrorFamily::class.qualifiedName!!)
    } == true
}

fun PsiClass.hasJimmerAnnotation(): Boolean {
    return this.hasImmutableAnnotation() || this.hasErrorFamilyAnnotation()
}

fun KtClass.hasImmutableAnnotation(): Boolean {
    return PSI_SHARED.annotations(this).any { annotation ->
        val fqName = annotation.fqName
        fqName == Immutable::class.qualifiedName!!
                || fqName == Entity::class.qualifiedName!!
                || fqName == MappedSuperclass::class.qualifiedName!!
                || fqName == Embeddable::class.qualifiedName!!
    } == true
}

fun KtClass.hasErrorFamilyAnnotation(): Boolean {
    return PSI_SHARED.annotations(this).any { annotation ->
        annotation.fqName == ErrorFamily::class.qualifiedName!!
    } == true
}

fun KtClass.hasJimmerAnnotation(): Boolean {
    return this.hasImmutableAnnotation() || this.hasErrorFamilyAnnotation()
}

fun PsiMethod.hasToManyAnnotation(): Boolean {
    return this.modifierList.annotations.any { annotation ->
        annotation.hasQualifiedName(OneToMany::class.qualifiedName!!)
                || annotation.hasQualifiedName(ManyToMany::class.qualifiedName!!)
    } == true
}

fun PsiMethod.hasToOneAnnotation(): Boolean {
    return this.modifierList.annotations.any { annotation ->
        annotation.hasQualifiedName(OneToOne::class.qualifiedName!!)
                || annotation.hasQualifiedName(ManyToOne::class.qualifiedName!!)
    } == true
}

fun PsiMethod.hasIdViewAnnotation(): Boolean {
    return this.modifierList.annotations.any { annotation ->
        annotation.hasQualifiedName(IdView::class.qualifiedName!!)
    } == true
}

fun KtProperty.hasToManyAnnotation(): Boolean {
    return PSI_SHARED.annotations(this).any { annotation ->
        val fqName = annotation.fqName
        fqName == OneToMany::class.qualifiedName!!
                || fqName == ManyToMany::class.qualifiedName!!
    } == true
}

fun KtProperty.hasToOneAnnotation(): Boolean {
    return PSI_SHARED.annotations(this).any { annotation ->
        val fqName = annotation.fqName
        fqName == OneToOne::class.qualifiedName!!
                || fqName == ManyToOne::class.qualifiedName!!
    } == true
}

fun KtProperty.hasIdViewAnnotation(): Boolean {
    return PSI_SHARED.annotations(this).any { annotation ->
        val fqName = annotation.fqName
        fqName == IdView::class.qualifiedName!!
    } == true
}

fun PsiMethod.getTarget(): PsiClass? {
    return this.returnType?.let { PsiUtil.resolveGenericsClassInType(it) }?.let {
        if (it.substitutor != PsiSubstitutor.EMPTY) {
            it.element?.typeParameters?.firstOrNull()?.let { parameter ->
                PsiUtil.resolveGenericsClassInType(it.substitutor.substitute(parameter)).element
            }
        } else {
            it.element
        }
    }
}

fun KtProperty.getTarget(): KtClass? {
    return this.typeReference?.let { PSI_SHARED.type(it) }?.let {
        if (it.arguments.isNotEmpty()) {
            it.arguments.firstOrNull()?.ktClass
        } else {
            it.ktClass
        }
    }
}