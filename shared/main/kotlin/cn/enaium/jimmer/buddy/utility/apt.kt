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

import cn.enaium.jimmer.buddy.Utility
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiEnumConstant
import com.jetbrains.rd.util.firstOrNull
import org.babyfish.jimmer.Formula
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.error.ErrorFamily
import org.babyfish.jimmer.sql.*
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.Writer
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.type.*
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.*
import javax.tools.JavaFileObject.Kind
import kotlin.io.path.Path
import kotlin.reflect.KClass

/**
 * @author Enaium
 */
data class Apt(
    val processingEnvironment: ProcessingEnvironment,
    val typeElements: Set<TypeElement>,
    val sources: List<Source>,
)

fun psiClassesToApt(psiClasses: CopyOnWriteArrayList<PsiClass>): Apt {
    val typeElementCaches = mutableMapOf<String, TypeElement>()
    psiClasses.forEach { psiClass ->
        typeElementCaches[psiClass.qualifiedName!!] = createTypeElement(
            getEnclosedElements = {
                if (psiClass.isInterface) {
                    psiClass.methods.map { method ->
                        createExecutableElement(
                            getKind = { ElementKind.METHOD },
                            getSimpleName = { createName(method.name) },
                            getModifiers = { setOf(Modifier.PUBLIC) },
                            getEnclosingElement = { typeElementCaches[psiClass.qualifiedName]!! },
                            getParameters = { emptyList() },
                            getReturnType = {
                                val canonicalText = method.returnType?.canonicalText
                                    ?: throw IllegalArgumentException("Unknown type: ${method.returnType}")

                                when (canonicalText) {
                                    "long" -> createPrimitiveType(getKind = { TypeKind.LONG })
                                    "int" -> createPrimitiveType(getKind = { TypeKind.INT })
                                    "short" -> createPrimitiveType(getKind = { TypeKind.SHORT })
                                    "byte" -> createPrimitiveType(getKind = { TypeKind.BYTE })
                                    "char" -> createPrimitiveType(getKind = { TypeKind.CHAR })
                                    "double" -> createPrimitiveType(getKind = { TypeKind.DOUBLE })
                                    "float" -> createPrimitiveType(getKind = { TypeKind.FLOAT })
                                    "boolean" -> createPrimitiveType(getKind = { TypeKind.BOOLEAN })
                                    "void" -> createPrimitiveType(getKind = { TypeKind.VOID })
                                    else -> null
                                }?.also { return@createExecutableElement it }
                                val returnType = canonicalText.substringBefore("<")
                                val returnTypeParameter = canonicalText.substringAfter("<").substringBefore(">")
                                createDeclaredType(
                                    getQualifiedName = { returnType },
                                    asElement = {
                                        if (canonicalText.contains("<")) {
                                            createTypeElement(
                                                getQualifiedName = { createName(returnType) },
                                                getSimpleName = {
                                                    createName(
                                                        returnType.substringAfterLast(".")
                                                    )
                                                },
                                                getKind = { ElementKind.CLASS },
                                                getModifiers = { setOf(Modifier.PUBLIC) },
                                                getEnclosedElements = { emptyList() },
                                                getAnnotation = { null },
                                                getAnnotationsByType = { emptyArray() },
                                                getEnclosingElement = {
                                                    createPackageElement(
                                                        getQualifiedName = {
                                                            createName(
                                                                canonicalText.substringBeforeLast(
                                                                    "."
                                                                )
                                                            )
                                                        }
                                                    )
                                                },
                                                getSuperclass = {
                                                    if (returnType == "java.util.List") {
                                                        createDeclaredType(
                                                            getQualifiedName = { "java.util.Collection" },
                                                            asElement = {
                                                                createTypeElement(
                                                                    getQualifiedName = { createName("java.util.Collection") },
                                                                )
                                                            })
                                                    } else {
                                                        null
                                                    }
                                                },
                                                getInterfaces = { emptyList() }
                                            )
                                        } else {
                                            typeElementCaches[canonicalText]
                                                ?: createTypeElement(
                                                    getQualifiedName = { createName(canonicalText) },
                                                    getSimpleName = { createName(canonicalText.substringAfterLast(".")) },
                                                    getKind = { ElementKind.CLASS },
                                                    getModifiers = { setOf(Modifier.PUBLIC) },
                                                    getEnclosingElement = {
                                                        createPackageElement(
                                                            getQualifiedName = {
                                                                createName(
                                                                    canonicalText.substringBeforeLast(
                                                                        "."
                                                                    )
                                                                )
                                                            }
                                                        )
                                                    },
                                                    getEnclosedElements = { emptyList() },
                                                    getAnnotation = { null },
                                                    getAnnotationsByType = { emptyArray() },
                                                    getSuperclass = { null },
                                                    getInterfaces = { emptyList() }
                                                )
                                        }
                                    },
                                    getTypeArguments = {
                                        if (canonicalText.contains("<")) {
                                            listOf(
                                                createDeclaredType(
                                                    getQualifiedName = {
                                                        returnTypeParameter
                                                    },
                                                    asElement = {
                                                        typeElementCaches[returnTypeParameter]
                                                            ?: typeElementCaches.filter { it.key.substringAfterLast(".") == returnTypeParameter }
                                                                .firstOrNull()?.value
                                                            ?: throw IllegalArgumentException("Unknown type: $returnTypeParameter")
                                                    },
                                                    getTypeArguments = {
                                                        emptyList()
                                                    }
                                                ))
                                        } else {
                                            emptyList()
                                        }
                                    }
                                )
                            },
                            getAnnotation = { anno ->
                                if (method.modifierList.annotations.any { it.hasQualifiedName(anno.name) } == true) {
                                    getAnnotation(
                                        anno
                                    )
                                } else {
                                    null
                                }
                            },
                            getAnnotationMirrors = {
                                method.modifierList.annotations.mapNotNull {
                                    if (it.hasQualifiedName(OneToMany::class.qualifiedName!!)) {
                                        createAnnotationMirror(OneToMany::class)
                                    } else if (it.hasQualifiedName(ManyToOne::class.qualifiedName!!)) {
                                        createAnnotationMirror(ManyToOne::class)
                                    } else if (it.hasQualifiedName(ManyToMany::class.qualifiedName!!)) {
                                        createAnnotationMirror(ManyToMany::class)
                                    } else if (it.hasQualifiedName(OneToOne::class.qualifiedName!!)) {
                                        createAnnotationMirror(OneToOne::class)
                                    } else {
                                        null
                                    }
                                }
                            },
                            getAnnotationsByType = { emptyArray() },
                            isDefault = { false }
                        )
                    }
                } else if (psiClass.isEnum) {
                    psiClass.children.filter { it is PsiEnumConstant }.map { it as PsiEnumConstant }
                        .map {
                            val enumConstant = createTypeElement(
                                getQualifiedName = { createName(it.name) },
                                getSimpleName = { createName(it.name) },
                                getKind = { ElementKind.ENUM_CONSTANT },
                                getEnclosingElement = { createTypeElement() },
                            )
                            createTypeElement(
                                getQualifiedName = { createName(it.name) },
                                getSimpleName = { createName(it.name) },
                                getKind = { ElementKind.ENUM_CONSTANT },
                                getEnclosingElement = { enumConstant },
                            )
                        }
                } else {
                    emptyList()
                }
            },
            getQualifiedName = { createName(psiClass.qualifiedName!!) },
            getSimpleName = { createName(psiClass.name!!) },
            getKind = {
                if (psiClass.isInterface) {
                    ElementKind.INTERFACE
                } else if (psiClass.isEnum) {
                    ElementKind.ENUM
                } else {
                    ElementKind.CLASS
                }
            },
            getModifiers = { setOf(Modifier.PUBLIC) },
            getAnnotation = { anno ->
                if (psiClass.modifierList?.annotations?.any { it.hasQualifiedName(anno.name) } == true) {
                    getAnnotation(
                        anno
                    )
                } else {
                    null
                }
            },
            getEnclosingElement = {
                createPackageElement(getQualifiedName = {
                    createName(
                        psiClass.qualifiedName!!.substringBeforeLast(".")
                    )
                })
            },
            asType = {
                createDeclaredType(
                    getQualifiedName = { psiClass.qualifiedName!! },
                    asElement = {
                        typeElementCaches[psiClass.qualifiedName!!]!!
                    },
                    getTypeArguments = { emptyList() }
                )
            },
            getInterfaces = {
                psiClass.interfaces.mapNotNull { element ->
                    if (element is PsiClass) {
                        createDeclaredType(
                            getQualifiedName = { element.qualifiedName!! },
                            asElement = {
                                typeElementCaches[element.qualifiedName]
                                    ?: throw IllegalArgumentException("Unknown type: ${element.qualifiedName}")
                            },
                            getTypeArguments = { emptyList() }
                        )
                    } else {
                        null
                    }
                }
            },
            getSuperclass = { null }
        )
    }

    val sources = mutableListOf<Source>()

    return Apt(
        object : ProcessingEnvironment {
            override fun getOptions(): Map<String, String> {
                return emptyMap()
            }

            override fun getMessager(): Messager {
                return object : Messager {
                    override fun printMessage(kind: Diagnostic.Kind, msg: CharSequence) {

                    }

                    override fun printMessage(
                        kind: Diagnostic.Kind,
                        msg: CharSequence,
                        e: Element
                    ) {

                    }

                    override fun printMessage(
                        kind: Diagnostic.Kind,
                        msg: CharSequence,
                        e: Element,
                        a: AnnotationMirror
                    ) {

                    }

                    override fun printMessage(
                        kind: Diagnostic.Kind,
                        msg: CharSequence,
                        e: Element,
                        a: AnnotationMirror,
                        v: AnnotationValue
                    ) {

                    }
                }
            }

            override fun getFiler(): Filer {
                return object : Filer {
                    override fun createSourceFile(
                        name: CharSequence,
                        vararg originatingElements: Element
                    ): JavaFileObject {
                        return object : SimpleJavaFileObject(
                            Path("dummy.java").toUri(),
                            Kind.OTHER
                        ) {
                            override fun openOutputStream(): OutputStream {
                                return object : ByteArrayOutputStream() {
                                    override fun close() {
                                        sources.add(
                                            Source(
                                                packageName = name.toString().substringBeforeLast("."),
                                                fileName = name.toString().substringAfterLast("."),
                                                extensionName = "java",
                                                content = toString()
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    override fun createClassFile(
                        name: CharSequence?,
                        vararg originatingElements: Element?
                    ): JavaFileObject? {
                        TODO("Not yet implemented")
                    }

                    override fun createResource(
                        location: JavaFileManager.Location?,
                        moduleAndPkg: CharSequence?,
                        relativeName: CharSequence?,
                        vararg originatingElements: Element?
                    ): FileObject? {
                        TODO("Not yet implemented")
                    }

                    override fun getResource(
                        location: JavaFileManager.Location,
                        moduleAndPkg: CharSequence,
                        relativeName: CharSequence
                    ): FileObject {
                        return object : SimpleJavaFileObject(
                            Path("dummy.txt").toUri(),
                            Kind.OTHER
                        ) {

                        }
                    }
                }
            }

            override fun getElementUtils(): Elements? {
                return object : Elements {
                    override fun getPackageElement(name: CharSequence): PackageElement {
                        TODO("Not yet implemented")
                    }

                    override fun getTypeElement(name: CharSequence): TypeElement? {
                        return typeElementCaches[name.toString()] ?: if (name == "java.lang.Number") {
                            val numberTypeElement =
                                createTypeElement(getQualifiedName = { createName("java.lang.Number") })
                            createTypeElement(
                                getQualifiedName = { createName("java.lang.Number") },
                                asType = {
                                    createDeclaredType(
                                        getQualifiedName = { "java.lang.Number" },
                                        asElement = { numberTypeElement })
                                })
                        } else {
                            null
                        }
                    }

                    override fun getElementValuesWithDefaults(a: AnnotationMirror): Map<out ExecutableElement, AnnotationValue> {
                        TODO("Not yet implemented")
                    }

                    override fun getDocComment(e: Element): String? {
                        return null
                    }

                    override fun isDeprecated(e: Element): Boolean {
                        TODO("Not yet implemented")
                    }

                    override fun getBinaryName(type: TypeElement): Name {
                        TODO("Not yet implemented")
                    }

                    override fun getPackageOf(e: Element): PackageElement {
                        TODO("Not yet implemented")
                    }

                    override fun getAllMembers(type: TypeElement): List<Element> {
                        TODO("Not yet implemented")
                    }

                    override fun getAllAnnotationMirrors(e: Element): List<AnnotationMirror> {
                        TODO("Not yet implemented")
                    }

                    override fun hides(
                        hider: Element,
                        hidden: Element
                    ): Boolean {
                        TODO("Not yet implemented")
                    }

                    override fun overrides(
                        overrider: ExecutableElement,
                        overridden: ExecutableElement,
                        type: TypeElement
                    ): Boolean {
                        TODO("Not yet implemented")
                    }

                    override fun getConstantExpression(value: Any): String {
                        TODO("Not yet implemented")
                    }

                    override fun printElements(w: Writer, vararg elements: Element) {
                        TODO("Not yet implemented")
                    }

                    override fun getName(cs: CharSequence): Name {
                        TODO("Not yet implemented")
                    }

                    override fun isFunctionalInterface(type: TypeElement): Boolean {
                        TODO("Not yet implemented")
                    }
                }
            }

            override fun getTypeUtils(): Types? {
                return object : Types {
                    override fun asElement(t: TypeMirror): Element? {
                        return typeElementCaches[t.toString()] ?: if (t.toString() == "java.util.List") {
                            createTypeElement(
                                getQualifiedName = { createName("java.util.List") },
                            )
                        } else {
                            null
                        }
                    }

                    override fun isSameType(
                        t1: TypeMirror?,
                        t2: TypeMirror?
                    ): Boolean {
                        TODO("Not yet implemented")
                    }

                    override fun isSubtype(
                        t1: TypeMirror,
                        t2: TypeMirror
                    ): Boolean {
                        return if (t1 is DeclaredType && t2 is DeclaredType) {
                            val t1Element = t1.asElement()
                            val t2Element = t2.asElement()

                            if (t1Element !is TypeElement || t2Element !is TypeElement) {
                                return false
                            }

                            var eq =
                                t1Element.qualifiedName == t2Element.qualifiedName

                            if (!eq) {
                                if (t2Element.qualifiedName.toString() == "java.lang.Number") {
                                    eq = when (t1Element.qualifiedName.toString()) {
                                        java.lang.Byte::class.qualifiedName,
                                        java.lang.Short::class.qualifiedName,
                                        java.lang.Integer::class.qualifiedName,
                                        java.lang.Long::class.qualifiedName,
                                        java.lang.Float::class.qualifiedName,
                                        java.lang.Double::class.qualifiedName,
                                        java.math.BigInteger::class.qualifiedName,
                                        java.math.BigDecimal::class.qualifiedName -> true

                                        else -> false
                                    }
                                }
                            }
                            eq
                        } else {
                            false
                        }
                    }

                    override fun isAssignable(
                        t1: TypeMirror?,
                        t2: TypeMirror?
                    ): Boolean {
                        TODO("Not yet implemented")
                    }

                    override fun contains(
                        t1: TypeMirror?,
                        t2: TypeMirror?
                    ): Boolean {
                        TODO("Not yet implemented")
                    }

                    override fun isSubsignature(
                        m1: ExecutableType?,
                        m2: ExecutableType?
                    ): Boolean {
                        TODO("Not yet implemented")
                    }

                    override fun directSupertypes(t: TypeMirror?): List<TypeMirror?>? {
                        TODO("Not yet implemented")
                    }

                    override fun erasure(t: TypeMirror?): TypeMirror? {
                        TODO("Not yet implemented")
                    }

                    override fun boxedClass(p: PrimitiveType?): TypeElement? {
                        TODO("Not yet implemented")
                    }

                    override fun unboxedType(t: TypeMirror?): PrimitiveType? {
                        TODO("Not yet implemented")
                    }

                    override fun capture(t: TypeMirror?): TypeMirror? {
                        TODO("Not yet implemented")
                    }

                    override fun getPrimitiveType(kind: TypeKind?): PrimitiveType? {
                        TODO("Not yet implemented")
                    }

                    override fun getNullType(): NullType? {
                        TODO("Not yet implemented")
                    }

                    override fun getNoType(kind: TypeKind?): NoType? {
                        TODO("Not yet implemented")
                    }

                    override fun getArrayType(componentType: TypeMirror?): ArrayType? {
                        TODO("Not yet implemented")
                    }

                    override fun getWildcardType(
                        extendsBound: TypeMirror?,
                        superBound: TypeMirror?
                    ): WildcardType {
                        return object : WildcardType {
                            override fun getExtendsBound(): TypeMirror? {
                                TODO("Not yet implemented")
                            }

                            override fun getSuperBound(): TypeMirror? {
                                TODO("Not yet implemented")
                            }

                            override fun getKind(): TypeKind? {
                                TODO("Not yet implemented")
                            }

                            override fun getAnnotationMirrors(): List<AnnotationMirror?>? {
                                TODO("Not yet implemented")
                            }

                            override fun <A : Annotation?> getAnnotation(annotationType: Class<A?>?): A? {
                                TODO("Not yet implemented")
                            }

                            override fun <A : Annotation?> getAnnotationsByType(annotationType: Class<A?>?): Array<out A?>? {
                                TODO("Not yet implemented")
                            }

                            override fun <R : Any?, P : Any?> accept(
                                v: TypeVisitor<R?, P?>?,
                                p: P?
                            ): R? {
                                TODO("Not yet implemented")
                            }

                        }
                    }

                    override fun getDeclaredType(
                        typeElem: TypeElement?,
                        vararg typeArgs: TypeMirror
                    ): DeclaredType {
                        return object : DeclaredType {
                            override fun asElement(): Element? {
                                return typeElementCaches[typeElem?.qualifiedName.toString()]
                            }

                            override fun getEnclosingType(): TypeMirror? {
                                TODO("Not yet implemented")
                            }

                            override fun getTypeArguments(): List<TypeMirror?>? {
                                TODO("Not yet implemented")
                            }

                            override fun getKind(): TypeKind? {
                                TODO("Not yet implemented")
                            }

                            override fun getAnnotationMirrors(): List<AnnotationMirror?>? {
                                TODO("Not yet implemented")
                            }

                            override fun <A : Annotation?> getAnnotation(annotationType: Class<A?>?): A? {
                                TODO("Not yet implemented")
                            }

                            override fun <A : Annotation?> getAnnotationsByType(annotationType: Class<A?>?): Array<out A?>? {
                                TODO("Not yet implemented")
                            }

                            override fun <R : Any?, P : Any?> accept(
                                v: TypeVisitor<R?, P?>?,
                                p: P?
                            ): R? {
                                TODO("Not yet implemented")
                            }
                        }
                    }

                    override fun getDeclaredType(
                        containing: DeclaredType?,
                        typeElem: TypeElement?,
                        vararg typeArgs: TypeMirror?
                    ): DeclaredType? {
                        TODO("Not yet implemented")
                    }

                    override fun asMemberOf(
                        containing: DeclaredType?,
                        element: Element?
                    ): TypeMirror? {
                        TODO("Not yet implemented")
                    }

                }
            }

            override fun getSourceVersion(): SourceVersion? {
                TODO("Not yet implemented")
            }

            override fun getLocale(): Locale? {
                TODO("Not yet implemented")
            }

        },
        typeElementCaches.values.toSet(),
        sources
    )
}

private fun getAnnotation(type: Class<Annotation>): Annotation? = when (type) {
    Immutable::class.java -> Utility.immutable()
    Entity::class.java -> Utility.entity()
    MappedSuperclass::class.java -> Utility.mappedSuperclass()
    Embeddable::class.java -> Utility.embeddable()
    ErrorFamily::class.java -> Utility.errorFamily()
    Id::class.java -> Utility.id()
    IdView::class.java -> Utility.idView()
    Key::class.java -> Utility.key()
    Version::class.java -> Utility.version()
    Formula::class.java -> Utility.formula()
    OneToOne::class.java -> Utility.oneToOne()
    OneToMany::class.java -> Utility.oneToMany()
    ManyToOne::class.java -> Utility.manyToOne()
    ManyToMany::class.java -> Utility.manyToMany()
    Column::class.java -> Utility.column()
    GeneratedValue::class.java -> Utility.generatedValue()
    JoinColumn::class.java -> Utility.joinColumn()
    JoinTable::class.java -> Utility.joinTable()
    Transient::class.java -> Utility._transient()
    Serialized::class.java -> Utility.serialized()
    LogicalDeleted::class.java -> Utility.logicalDeleted()
    else -> throw IllegalArgumentException("Unknown annotation type: $type")
}

private fun createAnnotationMirror(
    annotationType: KClass<out Annotation>
): AnnotationMirror {
    return object : AnnotationMirror {
        override fun getAnnotationType(): DeclaredType {
            return createDeclaredType(
                getQualifiedName = { annotationType.qualifiedName!! },
                asElement = {
                    createTypeElement(
                        getQualifiedName = { createName(annotationType.qualifiedName!!) },
                        getSimpleName = { createName(annotationType.simpleName!!) },
                    )
                })
        }

        override fun getElementValues(): Map<out ExecutableElement, AnnotationValue> {
            return emptyMap()
        }
    }
}

private fun createName(name: String): Name {
    return object : Name {
        override fun contentEquals(cs: CharSequence): Boolean {
            return name.contentEquals(cs)
        }

        override val length: Int
            get() = name.length

        override fun get(index: Int): Char {
            return name[index]
        }

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
            return name.subSequence(startIndex, endIndex)
        }

        override fun toString(): String {
            return name
        }
    }
}

private fun createPackageElement(
    getQualifiedName: () -> Name = { TODO("Not yet implemented") },
    asType: () -> TypeMirror = { TODO("${getQualifiedName()} Not yet implemented") },
    getSimpleName: () -> Name = { TODO("${getQualifiedName()} Not yet implemented") },
    getEnclosedElements: () -> List<Element> = { TODO("${getQualifiedName()} Not yet implemented") },
    isUnnamed: () -> Boolean = { false },
    getEnclosingElement: () -> Element = { TODO("${getQualifiedName()} Not yet implemented") },
    getKind: () -> ElementKind = { ElementKind.PACKAGE },
    getModifiers: () -> Set<Modifier> = { TODO("${getQualifiedName()} Not yet implemented") },
    getAnnotationMirrors: () -> List<AnnotationMirror> = { TODO("${getQualifiedName()} Not yet implemented") },
    getAnnotation: (Class<Annotation>) -> Annotation? = { TODO("${getQualifiedName()} Not yet implemented") },
    getAnnotationsByType: (Class<Annotation>) -> Array<Annotation> = { TODO("${getQualifiedName()} Not yet implemented") },
): PackageElement {
    return object : PackageElement {
        override fun asType(): TypeMirror {
            return asType()
        }

        override fun getQualifiedName(): Name {
            return getQualifiedName()
        }

        override fun getSimpleName(): Name {
            return getSimpleName()
        }

        override fun getEnclosedElements(): List<Element> {
            return getEnclosedElements()
        }

        override fun isUnnamed(): Boolean {
            return isUnnamed()
        }

        override fun getEnclosingElement(): Element {
            return getEnclosingElement()
        }

        override fun getKind(): ElementKind {
            return getKind()
        }

        override fun getModifiers(): Set<Modifier> {
            return getModifiers()
        }

        override fun getAnnotationMirrors(): List<AnnotationMirror> {
            return getAnnotationMirrors()
        }

        override fun <A : Annotation> getAnnotation(annotationType: Class<A>): A? {
            return getAnnotation.invoke(annotationType as Class<Annotation>) as A?
        }

        override fun <A : Annotation> getAnnotationsByType(annotationType: Class<A>): Array<out A> {
            return getAnnotationsByType.invoke(annotationType as Class<Annotation>) as Array<out A>
        }

        override fun <R : Any, P : Any> accept(
            v: ElementVisitor<R, P>?,
            p: P?
        ): R? {
            return v?.visitPackage(this, p)
        }
    }
}

private fun createPrimitiveType(
    getKind: () -> TypeKind,
    getAnnotationMirrors: () -> List<AnnotationMirror> = { emptyList() },
    getAnnotation: (Class<Annotation>) -> Annotation? = { null },
    getAnnotationsByType: (Class<Annotation>) -> Array<Annotation> = { emptyArray() },
): PrimitiveType {
    return object : PrimitiveType {
        override fun getKind(): TypeKind {
            return getKind()
        }

        override fun getAnnotationMirrors(): List<AnnotationMirror> {
            return getAnnotationMirrors()
        }

        override fun <A : Annotation> getAnnotation(annotationType: Class<A>): A? {
            return getAnnotation.invoke(annotationType as Class<Annotation>) as A?
        }

        override fun <A : Annotation> getAnnotationsByType(annotationType: Class<A>): Array<out A> {
            return getAnnotationsByType.invoke(annotationType as Class<Annotation>) as Array<out A>
        }

        override fun <R : Any, P : Any> accept(
            v: TypeVisitor<R, P>?,
            p: P?
        ): R? {
            return v?.visitPrimitive(this, p)
        }
    }
}

private fun createDeclaredType(
    getQualifiedName: () -> String,
    getKind: () -> TypeKind = { TypeKind.DECLARED },
    asElement: () -> Element = { TODO("${getKind()} Not yet implemented") },
    getEnclosingType: () -> TypeMirror? = {
        object : TypeMirror {
            override fun getKind(): TypeKind {
                return TypeKind.NONE
            }

            override fun getAnnotationMirrors(): List<AnnotationMirror?>? {
                TODO("Not yet implemented")
            }

            override fun <A : Annotation?> getAnnotation(annotationType: Class<A?>?): A? {
                TODO("Not yet implemented")
            }

            override fun <A : Annotation?> getAnnotationsByType(annotationType: Class<A?>?): Array<out A?>? {
                TODO("Not yet implemented")
            }

            override fun <R : Any?, P : Any?> accept(
                v: TypeVisitor<R?, P?>?,
                p: P?
            ): R? {
                TODO("Not yet implemented")
            }
        }
    },
    getTypeArguments: () -> List<TypeMirror> = { TODO("${getKind()} Not yet implemented") },
    getAnnotationMirrors: () -> List<AnnotationMirror> = { TODO("${getKind()} Not yet implemented") },
    getAnnotation: (Class<Annotation>) -> Annotation? = { TODO("${getKind()} Not yet implemented") },
    getAnnotationsByType: (Class<Annotation>) -> Array<Annotation> = { TODO("${getKind()} Not yet implemented") },
): DeclaredType {
    return object : DeclaredType {
        override fun asElement(): Element {
            return asElement()
        }

        override fun getEnclosingType(): TypeMirror? {
            return getEnclosingType()
        }

        override fun getTypeArguments(): List<TypeMirror> {
            return getTypeArguments()
        }

        override fun getKind(): TypeKind {
            return getKind()
        }

        override fun getAnnotationMirrors(): List<AnnotationMirror> {
            return getAnnotationMirrors()
        }

        override fun <A : Annotation> getAnnotation(annotationType: Class<A>): A? {
            return getAnnotation.invoke(annotationType as Class<Annotation>) as A?
        }

        override fun <A : Annotation> getAnnotationsByType(annotationType: Class<A>): Array<A> {
            return getAnnotationsByType.invoke(annotationType as Class<Annotation>) as Array<A>
        }

        override fun <R : Any, P : Any> accept(
            v: TypeVisitor<R, P>?,
            p: P?
        ): R? {
            return v?.visitDeclared(this, p)
        }

        override fun toString(): String {
            return getQualifiedName()
        }
    }
}

private fun createExecutableElement(
    getSimpleName: () -> Name = { TODO("Not yet implemented") },
    asType: () -> TypeMirror = { TODO("${getSimpleName()} Not yet implemented") },
    getTypeParameters: () -> List<TypeParameterElement> = { TODO("${getSimpleName()} Not yet implemented") },
    getReturnType: () -> TypeMirror = { TODO("${getSimpleName()} Not yet implemented") },
    getParameters: () -> List<VariableElement> = { TODO("${getSimpleName()} Not yet implemented") },
    getReceiverType: () -> TypeMirror = { TODO("${getSimpleName()} Not yet implemented") },
    isVarArgs: () -> Boolean = { TODO("${getSimpleName()} Not yet implemented") },
    isDefault: () -> Boolean = { TODO("${getSimpleName()} Not yet implemented") },
    getThrownTypes: () -> List<TypeMirror> = { TODO("${getSimpleName()} Not yet implemented") },
    getDefaultValue: () -> AnnotationValue = { TODO("${getSimpleName()} Not yet implemented") },
    getEnclosingElement: () -> Element = { TODO("${getSimpleName()} Not yet implemented") },
    getKind: () -> ElementKind = { TODO("${getSimpleName()} Not yet implemented") },
    getModifiers: () -> Set<Modifier> = { TODO("${getSimpleName()} Not yet implemented") },
    getEnclosedElements: () -> List<Element> = { TODO("${getSimpleName()} Not yet implemented") },
    getAnnotationMirrors: () -> List<AnnotationMirror> = { TODO("${getSimpleName()} Not yet implemented") },
    getAnnotation: (Class<Annotation>) -> Annotation? = { TODO("${getSimpleName()} Not yet implemented") },
    getAnnotationsByType: (Class<Annotation>) -> Array<Annotation> = { TODO("${getSimpleName()} Not yet implemented") },
): ExecutableElement {
    return object : ExecutableElement {
        override fun asType(): TypeMirror {
            return asType()
        }

        override fun getTypeParameters(): List<TypeParameterElement> {
            return getTypeParameters()
        }

        override fun getReturnType(): TypeMirror {
            return getReturnType()
        }

        override fun getParameters(): List<VariableElement> {
            return getParameters()
        }

        override fun getReceiverType(): TypeMirror {
            return getReceiverType()
        }

        override fun isVarArgs(): Boolean {
            return isVarArgs()
        }

        override fun isDefault(): Boolean {
            return isDefault()
        }

        override fun getThrownTypes(): List<TypeMirror> {
            return getThrownTypes()
        }

        override fun getDefaultValue(): AnnotationValue {
            return getDefaultValue()
        }

        override fun getEnclosingElement(): Element {
            return getEnclosingElement()
        }

        override fun getSimpleName(): Name {
            return getSimpleName()
        }

        override fun getKind(): ElementKind {
            return getKind()
        }

        override fun getModifiers(): Set<Modifier> {
            return getModifiers()
        }

        override fun getEnclosedElements(): List<Element> {
            return getEnclosedElements()
        }

        override fun getAnnotationMirrors(): List<AnnotationMirror> {
            return getAnnotationMirrors()
        }

        override fun <A : Annotation> getAnnotation(annotationType: Class<A>): A? {
            return getAnnotation.invoke(annotationType as Class<Annotation>) as A?
        }

        override fun <A : Annotation> getAnnotationsByType(annotationType: Class<A>): Array<out A> {
            return getAnnotationsByType.invoke(annotationType as Class<Annotation>) as Array<out A>
        }

        override fun <R : Any, P : Any> accept(
            v: ElementVisitor<R, P>?,
            p: P?
        ): R? {
            return v?.visitExecutable(this, p)
        }
    }
}

private fun createTypeParameter(
    getSimpleName: () -> Name = { TODO("Not yet implemented") },
    asType: () -> TypeMirror = { TODO("Not yet implemented") },
    getGenericElement: () -> Element = { TODO("Not yet implemented") },
    getBounds: () -> List<TypeMirror> = { TODO("Not yet implemented") },
    getEnclosingElement: () -> Element = { TODO("Not yet implemented") },
    getKind: () -> ElementKind = { ElementKind.TYPE_PARAMETER },
    getModifiers: () -> Set<Modifier> = { TODO("Not yet implemented") },
    getEnclosedElements: () -> List<Element> = { TODO("Not yet implemented") },
    getAnnotationMirrors: () -> List<AnnotationMirror> = { TODO("Not yet implemented") },
    getAnnotation: (Class<Annotation>) -> Annotation? = { TODO("Not yet implemented") },
    getAnnotationsByType: (Class<Annotation>) -> Array<Annotation> = { TODO("Not yet implemented") },
): TypeParameterElement {
    return object : TypeParameterElement {
        override fun asType(): TypeMirror {
            return asType()
        }

        override fun getGenericElement(): Element {
            return getGenericElement()
        }

        override fun getBounds(): List<TypeMirror> {
            return getBounds()
        }

        override fun getEnclosingElement(): Element {
            return getEnclosingElement()
        }

        override fun getKind(): ElementKind {
            return getKind()
        }

        override fun getModifiers(): Set<Modifier> {
            return getModifiers()
        }

        override fun getSimpleName(): Name {
            return getSimpleName()
        }

        override fun getEnclosedElements(): List<Element> {
            return getEnclosedElements()
        }

        override fun getAnnotationMirrors(): List<AnnotationMirror> {
            return getAnnotationMirrors()
        }

        override fun <A : Annotation> getAnnotation(annotationType: Class<A>): A? {
            return getAnnotation.invoke(annotationType as Class<Annotation>) as A?
        }

        override fun <A : Annotation> getAnnotationsByType(annotationType: Class<A>): Array<out A> {
            return getAnnotationsByType.invoke(annotationType as Class<Annotation>) as Array<out A>
        }

        override fun <R : Any, P : Any> accept(
            v: ElementVisitor<R, P>?,
            p: P?
        ): R? {
            return v?.visitTypeParameter(this, p)
        }
    }
}

private fun createTypeElement(
    getQualifiedName: () -> Name = { TODO("Not yet implemented") },
    asType: () -> TypeMirror = { TODO("${getQualifiedName()} Not yet implemented") },
    getEnclosedElements: () -> List<Element> = { TODO("${getQualifiedName()} Not yet implemented") },
    getNestingKind: () -> NestingKind = { TODO("${getQualifiedName()} Not yet implemented") },
    getSimpleName: () -> Name = { TODO("${getQualifiedName()} Not yet implemented") },
    getSuperclass: () -> TypeMirror? = { TODO("${getQualifiedName()} Not yet implemented") },
    getInterfaces: () -> List<TypeMirror> = { TODO("${getQualifiedName()} Not yet implemented") },
    getTypeParameters: () -> List<TypeParameterElement> = { TODO("${getQualifiedName()} Not yet implemented") },
    getEnclosingElement: () -> Element = { TODO("${getQualifiedName()} Not yet implemented") },
    getKind: () -> ElementKind = { TODO("${getQualifiedName()} Not yet implemented") },
    getModifiers: () -> Set<Modifier> = { TODO("${getQualifiedName()} Not yet implemented") },
    getAnnotationMirrors: () -> List<AnnotationMirror> = { emptyList() },
    getAnnotation: (Class<Annotation>) -> Annotation? = { TODO("${getQualifiedName()} Not yet implemented") },
    getAnnotationsByType: (Class<Annotation>) -> Array<Annotation> = {
        getAnnotation(it)?.let { arrayOf(it) } ?: emptyArray()
    },
): TypeElement {
    return object : TypeElement {
        override fun asType(): TypeMirror {
            return asType()
        }

        override fun getEnclosedElements(): List<Element> {
            return getEnclosedElements()
        }

        override fun getNestingKind(): NestingKind {
            return getNestingKind()
        }

        override fun getQualifiedName(): Name {
            return getQualifiedName()
        }

        override fun getSimpleName(): Name {
            return getSimpleName()
        }

        override fun getSuperclass(): TypeMirror? {
            return getSuperclass()
        }

        override fun getInterfaces(): List<TypeMirror> {
            return getInterfaces()
        }

        override fun getTypeParameters(): List<TypeParameterElement> {
            return getTypeParameters()
        }

        override fun getEnclosingElement(): Element {
            return getEnclosingElement()
        }

        override fun getKind(): ElementKind {
            return getKind()
        }

        override fun getModifiers(): Set<Modifier> {
            return getModifiers()
        }

        override fun getAnnotationMirrors(): List<AnnotationMirror> {
            return getAnnotationMirrors()
        }

        override fun <A : Annotation> getAnnotation(annotationType: Class<A>): A? {
            return getAnnotation.invoke(annotationType as Class<Annotation>) as A?
        }

        override fun <A : Annotation> getAnnotationsByType(annotationType: Class<A>): Array<out A> {
            return getAnnotationsByType.invoke(annotationType as Class<Annotation>) as Array<out A>
        }

        override fun <R : Any, P : Any> accept(
            v: ElementVisitor<R, P>,
            p: P?
        ): R {
            return v.visitType(this, p)
        }

        override fun toString(): String {
            return getQualifiedName().toString()
        }
    }
}