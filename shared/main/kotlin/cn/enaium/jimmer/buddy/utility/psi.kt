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
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.error.ErrorFamily
import org.babyfish.jimmer.sql.Embeddable
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.MappedSuperclass
import org.jetbrains.kotlin.psi.KtClass

/**
 * @author Enaium
 */
fun PsiClass.isJimmerImmutableType(): Boolean {
    return this.modifierList?.annotations?.any { annotation ->
        annotation.hasQualifiedName(Immutable::class.qualifiedName!!)
                || annotation.hasQualifiedName(Entity::class.qualifiedName!!)
                || annotation.hasQualifiedName(MappedSuperclass::class.qualifiedName!!)
                || annotation.hasQualifiedName(Embeddable::class.qualifiedName!!)
                || annotation.hasQualifiedName(ErrorFamily::class.qualifiedName!!)
    } == true
}

fun KtClass.isJimmerImmutableType(): Boolean {
    return PSI_SHARED.annotations(this).any { annotation ->
        val fqName = annotation.fqName
        fqName == Immutable::class.qualifiedName!!
                || fqName == Entity::class.qualifiedName!!
                || fqName == MappedSuperclass::class.qualifiedName!!
                || fqName == Embeddable::class.qualifiedName!!
                || fqName == ErrorFamily::class.qualifiedName!!
    } == true
}