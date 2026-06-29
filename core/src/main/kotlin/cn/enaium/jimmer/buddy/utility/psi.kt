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

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.service.PsiService
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.findParentOfType
import org.babyfish.jimmer.Draft
import org.babyfish.jimmer.Formula
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.error.ErrorFamily
import org.babyfish.jimmer.spring.repo.support.AbstractKotlinRepository
import org.babyfish.jimmer.sql.*
import org.babyfish.jimmer.sql.ast.Executable
import org.babyfish.jimmer.sql.ast.query.TypedRootQuery
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.KExecutable
import org.babyfish.jimmer.sql.kt.ast.query.KTypedRootQuery
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.findPropertyByName
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.uast.*
import org.jspecify.annotations.Nullable
import kotlin.reflect.KClass

/**
 * @author Enaium
 */
fun UClass.hasEntityAnnotation(): Boolean {
    return this.uAnnotations.any { annotation ->
        annotation.qualifiedName == Entity::class.qualifiedName!!
    }
}

fun UClass.isEntity(): Boolean {
    return hasEntityAnnotation() && isInterface
}

fun KtClass.annotations(): List<PsiService.Annotation> {
    return JimmerBuddy.Services.PSI.annotations(this)
}

fun KtClass.findAnnotation(name: String): PsiService.Annotation? {
    return this.annotations().find { it.fqName == name }
}

fun KtProperty.annotations(): List<PsiService.Annotation> {
    return JimmerBuddy.Services.PSI.annotations(this)
}

fun KtProperty.findAnnotation(name: String): PsiService.Annotation? {
    return this.annotations().find { it.fqName == name }
}

fun KtTypeReference.type(): PsiService.Type {
    return JimmerBuddy.Services.PSI.type(this)
}

fun KtLambdaExpression.receiver(): KtClass? {
    return JimmerBuddy.Services.PSI.receiver(this)
}

fun PsiClass.hasAnnotation(vararg annotations: KClass<*>): Boolean {
    return this.modifierList?.annotations?.any { annotation ->
        annotations.map { it.qualifiedName }.any { it == annotation.qualifiedName }
    } == true
}

fun PsiClass.hasImmutableAnnotation(): Boolean {
    return this.hasAnnotation(Immutable::class, Entity::class, MappedSuperclass::class, Embeddable::class)
}

fun PsiClass.hasEntityAnnotation(): Boolean {
    return this.hasAnnotation(Entity::class)
}

fun PsiClass.hasMappedSuperclassAnnotation(): Boolean {
    return this.hasAnnotation(MappedSuperclass::class)
}

fun PsiClass.isImmutable(): Boolean {
    return hasImmutableAnnotation() && isInterface
}

fun PsiClass.isEntity(): Boolean {
    return hasEntityAnnotation() && isInterface
}

fun PsiClass.isMappedSuperclass(): Boolean {
    return hasMappedSuperclassAnnotation() && isInterface
}

fun PsiClass.isErrorFamily(): Boolean {
    return hasErrorFamilyAnnotation() && isEnum
}

fun PsiClass.hasErrorFamilyAnnotation(): Boolean {
    return this.hasAnnotation(ErrorFamily::class)
}

fun PsiClass.hasTypedTupleAnnotation(): Boolean {
    return this.hasAnnotation(TypedTuple::class)
}

fun KtClass.hasAnnotation(vararg annotations: KClass<*>): Boolean {
    return this.toUElementOfType<UClass>()?.uAnnotations?.any { uAnnotation ->
        annotations.map { it.qualifiedName }.any { it == uAnnotation.qualifiedName }
    } == true
}

fun KtClass.hasEntityAnnotation(): Boolean {
    return this.hasAnnotation(Entity::class)
}

fun KtClass.hasMappedSuperclassAnnotation(): Boolean {
    return this.hasAnnotation(MappedSuperclass::class)
}

fun KtClass.hasImmutableAnnotation(): Boolean {
    return this.hasAnnotation(Immutable::class, Entity::class, MappedSuperclass::class, Embeddable::class)
}

fun KtClass.isImmutable(): Boolean {
    return hasImmutableAnnotation() && isInterface()
}

fun KtClass.isEntity(): Boolean {
    return hasEntityAnnotation() && isInterface()
}

fun KtClass.isMappedSuperclass(): Boolean {
    return hasMappedSuperclassAnnotation() && isInterface()
}

fun KtClass.isErrorFamily(): Boolean {
    return hasErrorFamilyAnnotation() && isEnum()
}

fun KtClass.hasErrorFamilyAnnotation(): Boolean {
    return this.hasAnnotation(ErrorFamily::class)
}

fun KtClass.hasTypedTupleAnnotation(): Boolean {
    return this.hasAnnotation(TypedTuple::class)
}

fun KtClass.hasJimmerAnnotation(): Boolean {
    return this.hasImmutableAnnotation() || this.hasErrorFamilyAnnotation() || this.hasTypedTupleAnnotation()
}

fun PsiClass.hasJimmerAnnotation(): Boolean {
    return this.hasImmutableAnnotation() || this.hasErrorFamilyAnnotation() || this.hasTypedTupleAnnotation()
}

fun PsiMethod.hasAnnotation(vararg annotations: KClass<*>): Boolean {
    return annotations.any { anno -> anno.qualifiedName in modifierList.annotations.map { it.qualifiedName } }
}

fun PsiMethod.hasToManyAnnotation(): Boolean {
    return this.hasAnnotation(OneToMany::class, ManyToMany::class)
}

fun PsiMethod.hasToOneAnnotation(): Boolean {
    return this.hasAnnotation(OneToOne::class, ManyToOne::class)
}

fun PsiMethod.hasTransientAnnotation(): Boolean {
    return this.hasAnnotation(Transient::class)
}

fun PsiMethod.isComputed(): Boolean {
    return hasTransientAnnotation() || hasFormulaAnnotation()
}

fun PsiMethod.hasSerializedAnnotation(): Boolean {
    return this.modifierList.annotations.any { annotation ->
        annotation.hasQualifiedName(Serialized::class.qualifiedName!!)
    }
}

fun PsiMethod.hasFormulaAnnotation(): Boolean {
    return this.modifierList.annotations.any { annotation ->
        annotation.hasQualifiedName(Formula::class.qualifiedName!!)
    }
}

fun PsiMethod.hasManyToManyViewAnnotation(): Boolean {
    return this.modifierList.annotations.any { annotation ->
        annotation.hasQualifiedName(ManyToManyView::class.qualifiedName!!)
    }
}

fun PsiMethod.hasIdViewAnnotation(): Boolean {
    return this.modifierList.annotations.any { annotation ->
        annotation.hasQualifiedName(IdView::class.qualifiedName!!)
    }
}

fun PsiMethod.hasIdAnnotation(): Boolean {
    return this.hasAnnotation(Id::class)
}

fun PsiMethod.isNullable(): Boolean {
    return this.modifierList.annotations.any { annotation ->
        annotation.qualifiedName?.endsWith(Nullable::class.simpleName!!) == true
    } || returnType?.canonicalText in listOf(
        Long::class.javaObjectType.name,
        Int::class.javaObjectType.name,
        Short::class.javaObjectType.name,
        Byte::class.javaObjectType.name,
        Float::class.javaObjectType.name,
        Double::class.javaObjectType.name
    )
}

fun KtProperty.hasAnnotation(vararg annotations: KClass<*>): Boolean {
    return annotations.any { anno -> anno.qualifiedName in annotations().map { it.fqName } }
}

fun KtProperty.hasIdAnnotation(): Boolean {
    return hasAnnotation(Id::class)
}

fun KtProperty.hasToManyAnnotation(): Boolean {
    return hasAnnotation(OneToMany::class, ManyToMany::class)
}

fun KtProperty.hasToOneAnnotation(): Boolean {
    return hasAnnotation(OneToOne::class, ManyToOne::class)
}

fun KtProperty.hasTransientAnnotation(): Boolean {
    return hasAnnotation(Transient::class)
}

fun PsiMethod.findTransientNavigationTargets(): List<PsiElement> {
    if (!hasTransientAnnotation()) {
        return emptyList()
    }

    val annotation = modifierList.annotations.find { annotation ->
        annotation.hasQualifiedName(Transient::class.qualifiedName!!)
    } ?: return emptyList()

    val resolverTargets = annotation.findTransientResolverTargets()
    if (resolverTargets.isNotEmpty()) {
        return resolverTargets
    }

    val generatedClass = containingClass?.findGeneratedClass() ?: return emptyList()
    return generatedClass.findMethodsByName(name, false).toList()
}

fun KtProperty.findTransientNavigationTargets(): List<PsiElement> {
    if (!hasTransientAnnotation()) {
        return emptyList()
    }

    val annotation = annotationEntries.find { entry ->
        entry.toUElementOfType<UAnnotation>()?.qualifiedName == Transient::class.qualifiedName
    } ?: return emptyList()

    val resolverTargets = annotation.findTransientResolverTargets()
    if (resolverTargets.isNotEmpty()) {
        return resolverTargets
    }

    val propertyName = name ?: return emptyList()
    val generatedClass = containingClass()?.findGeneratedClass() ?: return emptyList()
    return listOfNotNull(generatedClass.findPropertyByName(propertyName))
}

private fun PsiAnnotation.findTransientResolverTargets(): List<PsiElement> {
    val targets = mutableListOf<PsiElement>()
    findClassAttributeTarget("value")?.also { target ->
        targets.add(target)
    }
    findStringAttribute("ref")?.also { ref ->
        targets.addAll(project.findSpringBeanTargets(ref))
    }
    return targets.distinct()
}

private fun KtAnnotationEntry.findTransientResolverTargets(): List<PsiElement> {
    val targets = mutableListOf<PsiElement>()
    findClassArgumentTarget("value")?.also { target ->
        targets.add(target)
    }
    findStringArgument("ref")?.also { ref ->
        targets.addAll(project.findSpringBeanTargets(ref))
    }
    return targets.distinct()
}

private fun PsiAnnotation.findClassAttributeTarget(attributeName: String): PsiClass? {
    val value = findDeclaredAttributeValue(attributeName) ?: return null
    return (value as? PsiClassObjectAccessExpression)?.operand?.type?.resolveClass()
}

private fun PsiAnnotation.findStringAttribute(attributeName: String): String? {
    val value = findDeclaredAttributeValue(attributeName) ?: return null
    return value.stringValues().firstOrNull { it.isNotBlank() }
}

private fun PsiAnnotationMemberValue.stringValues(): List<String> {
    return when (this) {
        is PsiLiteralExpression -> listOfNotNull(value as? String)
        is PsiArrayInitializerMemberValue -> initializers.flatMap { it.stringValues() }
        else -> emptyList()
    }
}

private fun KtAnnotationEntry.findClassArgumentTarget(argumentName: String): PsiElement? {
    val expression = findArgumentExpression(argumentName) as? KtClassLiteralExpression ?: return null
    val receiver = expression.receiverExpression ?: return null
    return receiver.lastReferenceExpression()?.reference?.resolve()
}

private fun KtAnnotationEntry.findStringArgument(argumentName: String): String? {
    val expression = findArgumentExpression(argumentName) ?: return null
    return expression.toUElementOfType<UExpression>()?.evaluate()?.toString()?.takeIf { it.isNotBlank() }
}

private fun KtAnnotationEntry.findArgumentExpression(argumentName: String): KtExpression? {
    val namedArgument = valueArguments.firstOrNull { argument ->
        argument.getArgumentName()?.asName?.identifier == argumentName
    }
    if (namedArgument != null) {
        return namedArgument.getArgumentExpression()
    }

    if (argumentName != "value") {
        return null
    }

    return valueArguments.firstOrNull { argument ->
        argument.getArgumentName() == null
    }?.getArgumentExpression()
}

private fun KtExpression.lastReferenceExpression(): KtReferenceExpression? {
    return when (this) {
        is KtReferenceExpression -> this
        is KtDotQualifiedExpression -> selectorExpression as? KtReferenceExpression
        else -> null
    }
}

fun Project.findSpringBeanTargets(beanName: String): List<PsiElement> {
    val scope = GlobalSearchScope.projectScope(this)
    val cache = PsiShortNamesCache.getInstance(this)
    val targets = mutableListOf<PsiElement>()

    targets.addAll(cache.getMethodsByName(beanName, scope).filter { method ->
        method.isSpringBean(beanName)
    })
    targets.addAll(cache.getClassesByName(beanName.toClassNameCandidate(), scope).filter { psiClass ->
        psiClass.isSpringBean(beanName)
    })

    if (targets.isNotEmpty()) {
        return targets.distinct()
    }

    cache.allMethodNames.asSequence()
        .flatMap { name -> cache.getMethodsByName(name, scope).asSequence() }
        .filter { method -> method.isSpringBean(beanName) }
        .forEach { method -> targets.add(method) }

    cache.allClassNames.asSequence()
        .flatMap { name -> cache.getClassesByName(name, scope).asSequence() }
        .filter { psiClass -> psiClass.isSpringBean(beanName) }
        .forEach { psiClass -> targets.add(psiClass) }

    return targets.distinct()
}

private fun PsiClass.isSpringBean(beanName: String): Boolean {
    val annotation = modifierList?.annotations?.find { annotation ->
        annotation.qualifiedName in springComponentAnnotations
    } ?: return false

    val explicitNames = annotation.stringValues("value")
    if (explicitNames.isNotEmpty()) {
        return beanName in explicitNames
    }

    return name?.toSpringBeanName() == beanName
}

private fun PsiMethod.isSpringBean(beanName: String): Boolean {
    val annotation = modifierList.annotations.find { annotation ->
        annotation.qualifiedName in springBeanAnnotations
    } ?: return false

    val explicitNames = annotation.stringValues("name") + annotation.stringValues("value")
    if (explicitNames.isNotEmpty()) {
        return beanName in explicitNames
    }

    return name == beanName
}

private fun PsiAnnotation.stringValues(attributeName: String): List<String> {
    val value = findDeclaredAttributeValue(attributeName) ?: return emptyList()
    return value.stringValues()
}

private fun String.toClassNameCandidate(): String {
    return replaceFirstChar { char ->
        if (char.isLowerCase()) {
            char.titlecase()
        } else {
            char.toString()
        }
    }
}

private fun String.toSpringBeanName(): String {
    if (length > 1 && this[0].isUpperCase() && this[1].isUpperCase()) {
        return this
    }

    return replaceFirstChar { char -> char.lowercase() }
}

private val springComponentAnnotations = setOf(
    "org.springframework.stereotype.Component",
    "org.springframework.stereotype.Service",
    "org.springframework.stereotype.Repository",
    "org.springframework.stereotype.Controller",
    "org.springframework.web.bind.annotation.RestController",
    "org.springframework.context.annotation.Configuration",
    "jakarta.inject.Named",
    "javax.inject.Named",
)

private val springBeanAnnotations = setOf(
    "org.springframework.context.annotation.Bean",
)

fun KtProperty.isComputed(): Boolean {
    return hasTransientAnnotation() || hasFormulaAnnotation()
}

fun KtProperty.hasSerializedAnnotation(): Boolean {
    return hasAnnotation(Serialized::class)
}

fun KtProperty.hasFormulaAnnotation(): Boolean {
    return hasAnnotation(Formula::class)
}

fun KtProperty.hasManyToManyViewAnnotation(): Boolean {
    return hasAnnotation(ManyToManyView::class)
}

fun KtProperty.hasIdViewAnnotation(): Boolean {
    return hasAnnotation(IdView::class)
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

fun PsiMethod.getTargetName(unbox: Boolean = false): String? {
    return if (this.returnType is PsiPrimitiveType) {
        (this.returnType as? PsiPrimitiveType)?.name
    } else {
        this.getTarget()?.qualifiedName
    }?.let {
        if (unbox) {
            if (it in listOf(
                    java.lang.Long::class.java.name,
                    java.lang.Integer::class.java.name,
                    java.lang.Short::class.java.name,
                    java.lang.Byte::class.java.name,
                    java.lang.Float::class.java.name,
                    java.lang.Double::class.java.name,
                )
            ) {
                it.substringAfterLast(".").lowercase()
            } else {
                it
            }
        } else {
            it
        }
    }
}

fun KtProperty.getTarget(): KtClass? {
    return this.typeReference?.type()?.let {
        if (it.arguments.isNotEmpty()) {
            it.firstArgType()?.ktClass
        } else {
            it.ktClass
        }
    }
}

fun PsiClass.hasTableAnnotation(): Boolean {
    return this.modifierList?.annotations?.any { annotation ->
        annotation.hasQualifiedName(Table::class.qualifiedName!!)
    } == true
}

fun KtClass.hasTableAnnotation(): Boolean {
    return this.toUElementOfType<UClass>()?.uAnnotations?.any { annotation ->
        val fqName = annotation.qualifiedName
        fqName == Table::class.qualifiedName!!
    } == true
}

fun PsiMethod.hasColumnAnnotation(): Boolean {
    return this.modifierList.annotations.any { annotation ->
        annotation.hasQualifiedName(Column::class.qualifiedName!!)
    }
}

fun KtProperty.hasColumnAnnotation(): Boolean {
    return this.annotations().any { annotation ->
        val fqName = annotation.fqName
        fqName == Column::class.qualifiedName!!
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
    return this.getParentOfType<PsiClass>(true)?.isImmutable() == true || this.getParentOfType<KtClass>(true)
        ?.isImmutable() == true
}

fun PsiElement.isAnnotation(name: String): Boolean {
    return (this is PsiAnnotation || this is KtAnnotationEntry || this is KtCallExpression) && this.toUElementOfType<UAnnotation>()?.qualifiedName == name
}

fun PsiElement.getComment(): String? {
    var comment = ""

    getChildOfType<KDoc>()?.also { doc ->
        comment = doc.getAllSections().filter { it.text != "*" && it.text.isNotBlank() }.joinToString("\n") { section ->
            section.text.trim().let {
                if (it.startsWith("* ")) {
                    it.substring(2)
                } else {
                    it
                }
            }
        }
    }

    getChildOfType<PsiDocComment>()?.also { doc ->
        comment = doc.descriptionElements.filter { it.text != "*" && it.text.isNotBlank() }.joinToString("\n") {
            it.text.trim()
        }
    }

    return comment.takeIf { it.isNotEmpty() }
}

fun PsiType.resolveClass(): PsiClass? {
    return PsiUtil.resolveClassInClassTypeOnly(this)
}

fun PsiType.resolveGenericsClassInType(): PsiClassType.ClassResolveResult {
    return PsiUtil.resolveGenericsClassInType(this)
}

fun PsiType.resolveGenericsClass(parameter: PsiTypeParameter): PsiClass? {
    return PsiUtil.resolveGenericsClassInType(this).substitutor.substitute(parameter)?.resolveClass()
}

fun PsiMethodCallExpression.findExecuteMethod(): PsiMethodCallExpression? {
    val containingClass = resolveMethod()?.containingClass
    return if (listOf(
            TypedRootQuery::class.qualifiedName,
            Executable::class.qualifiedName,
            JSqlClient::class.qualifiedName
        ).any { it == containingClass?.qualifiedName }
    ) {
        this
    } else {
        val child = firstChild?.firstChild
        if (child is PsiMethodCallExpression) {
            child.findExecuteMethod()
        } else if (child is PsiReferenceExpression) {
            val resolve = child.resolve()
            if (resolve is PsiLocalVariable) {
                resolve.getChildOfType<PsiMethodCallExpression>()?.findExecuteMethod()
            } else if (resolve is PsiParameter && resolve.declarationScope is PsiLambdaExpression) {
                resolve.findParentOfType<PsiLambdaExpression>()?.findParentOfType<PsiMethodCallExpression>()
                    ?.findExecuteMethod()
            } else {
                null
            }
        } else {
            null
        }
    }
}

fun KtQualifiedExpression.findExecuteFun(): KtQualifiedExpression? {
    val selectorExpression = this@findExecuteFun.selectorExpression as? KtCallExpression
    val receiverExpression = this@findExecuteFun.receiverExpression as? KtCallExpression
    return (if (selectorExpression != null && listOf(
            KTypedRootQuery::class.qualifiedName,
            KExecutable::class.qualifiedName,
            KSqlClient::class.qualifiedName
        ).any { it == (selectorExpression.firstChild?.reference?.resolve() as? KtNamedFunction)?.containingClass()?.fqName?.asString() }
    ) {
        this
    } else if (receiverExpression != null && listOf(
            AbstractKotlinRepository::class.qualifiedName
        ).any { it == (receiverExpression.firstChild?.reference?.resolve() as? KtNamedFunction)?.containingClass()?.fqName?.asString() }
    ) {
        this
    } else if (firstChild is KtQualifiedExpression) {
        (firstChild as? KtQualifiedExpression)?.findExecuteFun()
    } else if (firstChild is KtArrayAccessExpression) {
        (firstChild.firstChild as? KtQualifiedExpression)?.findExecuteFun()
            ?: (firstChild.firstChild as? KtNameReferenceExpression)?.findExecuteFun()
    } else if (firstChild is KtNameReferenceExpression) {
        (firstChild as? KtNameReferenceExpression)?.findExecuteFun()
    } else {
        null
    })
}

fun KtNameReferenceExpression.findExecuteFun(): KtQualifiedExpression? {
    return when (val resolve = (firstChild.reference ?: reference)?.resolve()) {
        is KtProperty -> {
            resolve.getChildOfType<KtQualifiedExpression>()?.findExecuteFun()
        }

        is KtFunctionLiteral -> {
            resolve.findParentOfType<KtQualifiedExpression>()?.findExecuteFun()
        }

        else -> {
            null
        }
    }
}

fun UClass.getTableName(): String? {
    return uAnnotations.find { it.qualifiedName.equals(Table::class.qualifiedName, ignoreCase = true) }
        ?.findAttributeValue("name")
        ?.string() ?: qualifiedName?.substringAfterLast(".")?.camelToSnakeCase()
}


fun PsiMethodCallExpression.getImmutableTrace(execute: PsiMethodCallExpression? = null): List<String> {
    val trace = mutableListOf<String>()

    var child: PsiElement? = this

    while (child != null) {
        if (child is PsiMethodCallExpression) {
            val resolveMethod = child.resolveMethod()
            if (resolveMethod?.containingClass?.isImmutable() == true) {
                trace.add(resolveMethod.name)
            } else if (child == execute) {
                return trace.reversed()
            }
        }

        child = if (child is PsiReferenceExpression) {
            when (val resolve = child.reference?.resolve()) {
                is PsiParameter if resolve.declarationScope is PsiLambdaExpression -> {
                    child.findParentOfType<PsiLambdaExpression>()?.findParentOfType<PsiMethodCallExpression>()
                }

                is PsiLocalVariable if resolve.initializer is PsiMethodCallExpression -> {
                    resolve.initializer
                }

                else -> {
                    child.firstChild?.firstChild
                }
            }
        } else {
            child.firstChild?.firstChild
        }
    }

    return trace.reversed()
}

fun KtQualifiedExpression.getImmutableTrace(execute: KtQualifiedExpression? = null): List<String> {
    val trace = mutableListOf<String>()

    var child: PsiElement? = this

    while (child != null) {
        if (child is KtQualifiedExpression) {
            val property = child.lastChild.reference?.resolve() as? KtProperty
            if (property?.isMember == true && property.containingClass()?.isImmutable() == true) {
                trace.add(property.name ?: continue)
            } else if (child == execute) {
                return trace.reversed()
            }
        } else if (child is KtNameReferenceExpression) {
            val property = child.reference?.resolve() as? KtProperty
            if (property?.isMember == true && property.containingClass()?.isImmutable() == true) {
                trace.add(property.name ?: continue)
            }
        }
        child = if (child is KtNameReferenceExpression) {
            when (val resolve = child.reference?.resolve()) {
                is KtFunctionLiteral -> child.findParentOfType<KtLambdaExpression>()?.findParentOfType<KtQualifiedExpression>()
                is KtParameter -> resolve.findParentOfType<KtLambdaExpression>()?.findParentOfType<KtQualifiedExpression>()
                else -> child.firstChild
            }
        } else {
            child.firstChild
        }
    }

    return trace.reversed()
}

fun PsiClass.findIdMethod(): PsiMethod? {
    return allMethods.find { it.hasIdAnnotation() }
}

fun KtClass.findIdProperty(): KtProperty? {
    return getAllProperties().find { it.hasIdAnnotation() }
}

fun PsiClass.findGeneratedClass(): PsiClass? {
    val qualifiedName = this.qualifiedName ?: return null
    val scope = project.allScope()
    val facade = JavaPsiFacade.getInstance(project)
    val found = facade.findClass(qualifiedName, scope) ?: return null
    if (found.containingFile?.virtualFile?.let { isGeneratedSourceFile(it) } == true) {
        return found
    }

    val simpleName = qualifiedName.substringAfterLast('.')
    val generatedFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScope.projectScope(project))
        .asSequence()
        .filter { it.name == "$simpleName.java" && isGeneratedSourceFile(it) }
    return generatedFiles.firstNotNullOfOrNull { virtualFile ->
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return@firstNotNullOfOrNull null
        (psiFile as? PsiClassOwner)?.classes?.firstOrNull { it.qualifiedName == qualifiedName }
    }
}

fun KtClass.findGeneratedClass(): KtClass? {
    val fqName = this.fqName?.asString() ?: return null
    val scope = project.allScope()
    val allClasses = KotlinFullClassNameIndex[fqName, project, scope]
    return allClasses.firstOrNull { ktClass ->
        val virtualFile = ktClass.containingFile.virtualFile ?: return@firstOrNull false
        isGeneratedSourceFile(virtualFile)
    } as? KtClass
}

private fun isGeneratedSourceFile(virtualFile: VirtualFile): Boolean {
    var current = virtualFile.toNioPath().parent
    while (current != null) {
        val name = current.fileName?.toString()
        if (name == "generated" || name == "generated-sources") {
            return true
        }
        current = current.parent
    }
    return false
}
