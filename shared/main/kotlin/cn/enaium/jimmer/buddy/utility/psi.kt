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
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.util.PsiUtil
import org.babyfish.jimmer.Draft
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.error.ErrorFamily
import org.babyfish.jimmer.sql.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findPropertyByName
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.toUElementOfType

/**
 * @author Enaium
 */

fun KtClass.annotations(): List<PsiShared.Annotation> {
    return PSI_SHARED.annotations(this)
}

fun KtProperty.annotations(): List<PsiShared.Annotation> {
    return PSI_SHARED.annotations(this)
}

fun KtTypeReference.type(): PsiShared.Type {
    return PSI_SHARED.type(this)
}

fun KtLambdaExpression.receiver(): KtClass? {
    return PSI_SHARED.receiver(this)
}

fun PsiClass.hasImmutableAnnotation(): Boolean {
    return this.modifierList?.annotations?.any { annotation ->
        annotation.hasQualifiedName(Immutable::class.qualifiedName!!)
                || annotation.hasQualifiedName(Entity::class.qualifiedName!!)
                || annotation.hasQualifiedName(MappedSuperclass::class.qualifiedName!!)
                || annotation.hasQualifiedName(Embeddable::class.qualifiedName!!)
    } == true
}

fun PsiClass.isImmutable(): Boolean {
    return hasImmutableAnnotation() && isInterface
}

fun PsiClass.isErrorFamily(): Boolean {
    return hasErrorFamilyAnnotation() && isEnum
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
    return this.toUElementOfType<UClass>()?.uAnnotations?.any { annotation ->
        val fqName = annotation.qualifiedName
        fqName == Immutable::class.qualifiedName!!
                || fqName == Entity::class.qualifiedName!!
                || fqName == MappedSuperclass::class.qualifiedName!!
                || fqName == Embeddable::class.qualifiedName!!
    } == true
}

fun KtClass.isImmutable(): Boolean {
    return hasImmutableAnnotation() && isInterface()
}

fun KtClass.isErrorFamily(): Boolean {
    return hasErrorFamilyAnnotation() && isEnum()
}

fun KtClass.hasErrorFamilyAnnotation(): Boolean {
    return this.toUElementOfType<UClass>()?.uAnnotations?.any { annotation ->
        val fqName = annotation.qualifiedName
        fqName == ErrorFamily::class.qualifiedName!!
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
    return this.annotations().any { annotation ->
        val fqName = annotation.fqName
        fqName == OneToMany::class.qualifiedName!!
                || fqName == ManyToMany::class.qualifiedName!!
    } == true
}

fun KtProperty.hasToOneAnnotation(): Boolean {
    return this.annotations().any { annotation ->
        val fqName = annotation.fqName
        fqName == OneToOne::class.qualifiedName!!
                || fqName == ManyToOne::class.qualifiedName!!
    } == true
}

fun KtProperty.hasIdViewAnnotation(): Boolean {
    return this.annotations().any { annotation ->
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
    return this.typeReference?.type()?.let {
        if (it.arguments.isNotEmpty()) {
            it.arguments.firstOrNull()?.ktClass
        } else {
            it.ktClass
        }
    }
}

fun KtClass.findPropertyByName(name: String, superType: Boolean): KtNamedDeclaration? {
    val prop = this.findPropertyByName(name)
    if (prop == null && superType) {
        this.superTypeListEntries.forEach {
            it.typeReference?.type()?.ktClass?.also {
                if (it.isImmutable()) {
                    it.findPropertyByName(name, true)?.also {
                        return it
                    }
                }
            }
        }
    }
    return prop
}

fun KtClass.getAllProperties(): List<KtProperty> {
    val result = mutableListOf<KtProperty>()
    this.getProperties().forEach {
        result.add(it)
    }
    this.superTypeListEntries.forEach {
        it.typeReference?.type()?.ktClass?.also {
            if (it.isImmutable()) {
                result.addAll(it.getAllProperties())
            }
        }
    }
    return result
}

fun PsiClass.isDraft(): Boolean {
    superTypes.takeIf { it.isNotEmpty() }?.forEach {
        if (PsiUtil.resolveGenericsClassInType(it).element?.isDraft() == true) {
            return true
        }
    }

    return this.qualifiedName == Draft::class.qualifiedName
}

fun KtClass.isDraft(): Boolean {
    superTypeListEntries.takeIf { it.isNotEmpty() }?.forEach {
        it.typeReference?.also {
            it.type().also {
                if (it.ktClass?.isDraft() == true) {
                    return true
                } else if (it.fqName == Draft::class.qualifiedName) {
                    return true
                }
            }
        }
    }

    return this.fqName?.asString() == Draft::class.qualifiedName
}

fun PsiLambdaExpression.firstArg(): Pair<String, PsiClass?>? {
    return parameterList.parameters.firstOrNull()
        ?.let { it.name to PsiUtil.resolveGenericsClassInType(it.type).element }
}

fun PsiAnnotation.method(): PsiMethod? {
    return ((this.parent as? PsiModifierList)?.parent as? PsiMethod)
}

fun KtAnnotationEntry.property(): KtProperty? {
    return ((this.parent as? KtModifierList)?.parent as? KtProperty)
}

fun PsiElement.annotName(): String? {
    return (this.getParentOfType<PsiAnnotation>(true) ?: this.getParentOfType<KtCallExpression>(true)
    ?: this.getParentOfType<KtAnnotationEntry>(true)).toUElementOfType<UAnnotation>()?.qualifiedName
}

fun PsiElement.annotValue(attribute: String): UExpression? {
    return (this.getParentOfType<PsiAnnotation>(true) ?: this.getParentOfType<KtAnnotationEntry>(
        true
    )).toUElementOfType<UAnnotation>()
        ?.findAttributeValue(attribute)
}

fun UExpression.text() {

}

fun UExpression.string(): String? {
    return this.evaluate()?.toString()
}

fun UExpression.classLiteral(): String? {
    return ((this as? UClassLiteralExpression)?.type as? PsiClassReferenceType)?.canonicalText
}

fun PsiElement.annotArgName(): String? {
    this.getParentOfType<PsiNameValuePair>(true)?.also {
        return it.nameIdentifier?.text ?: "value"
    }
    this.getParentOfType<KtValueArgument>(true)?.also {
        return it.text.takeIf { it.contains("=") }?.substringBefore(" ") ?: "value"
    }
    return null
}

fun PsiElement.inImmutable(): Boolean {
    return this.getParentOfType<PsiClass>(true)?.isImmutable() != true || this.getParentOfType<KtClass>(true)
        ?.isImmutable() != true
}

fun PsiElement.isAnnotation(name: String): Boolean {
    return (this is PsiAnnotation || this is KtAnnotationEntry || this is KtCallExpression) && this.toUElementOfType<UAnnotation>()?.qualifiedName == name
}