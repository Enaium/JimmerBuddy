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
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.PsiUtil
import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.implementation.FixedValue
import net.bytebuddy.implementation.InvocationHandlerAdapter
import net.bytebuddy.matcher.ElementMatchers
import net.bytebuddy.matcher.ElementMatchers.named
import org.babyfish.jimmer.Formula
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.Scalar
import org.babyfish.jimmer.error.ErrorFamily
import org.babyfish.jimmer.error.ErrorField
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.sql.*
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.Writer
import java.util.*
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

/**
 * @author Enaium
 */
fun PsiClass.asTypeElement(caches: MutableMap<String, TypeElement> = mutableMapOf()): TypeElement {
    return caches[this.qualifiedName!!] ?: createTypeElement(
        getEnclosedElements = {
            if (this.isEnum) {
                this.getChildrenOfType<PsiEnumConstant>()
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
                this.methods.mapNotNull { method ->
                    method.asExecutableElement(caches)
                } + this.fields.mapNotNull {
                    it.asVariableElement(caches)
                }
            }
        },
        getQualifiedName = { createName(this.qualifiedName!!) },
        getSimpleName = { createName(this.name!!) },
        getKind = {
            if (this.isInterface) {
                ElementKind.INTERFACE
            } else if (this.isEnum) {
                ElementKind.ENUM
            } else if (this.isAnnotationType) {
                ElementKind.ANNOTATION_TYPE
            } else {
                ElementKind.CLASS
            }
        },
        getModifiers = { setOf(Modifier.PUBLIC) },
        getAnnotation = { anno ->
            this.modifierList?.annotations?.find { it.hasQualifiedName(anno.name) }?.findAnnotation()
        },
        getAnnotationMirrors = {
            this.modifierList?.annotations?.mapNotNull {
                it.takeIf { it.qualifiedName?.startsWith(jimmerAnnotationPrefix) == true }?.findAnnotation()?.let {
                    createAnnotationMirror(
                        it,
                        caches
                    )
                }
            } ?: emptyList()
        },
        getEnclosingElement = {
            createPackageElement(
                getQualifiedName = { createName(this.qualifiedName!!.substringBeforeLast(".")) }
            )
        },
        asType = {
            createDeclaredType(
                getQualifiedName = { this.qualifiedName!! },
                asElement = {
                    this.asTypeElement(caches)
                },
                getTypeArguments = {
                    this.typeParameters.mapNotNull {
                        it.asTypeMirror(caches)
                    }
                }
            )
        },
        getInterfaces = {
            this.interfaceTypes.mapNotNull { type ->
                if (type is PsiType) {
                    type.asTypeMirror(caches)
                } else {
                    null
                }
            }
        },
        getTypeParameters = {
            this.typeParameters.map {
                createTypeParameter(
                    getSimpleName = { createName(it.name!!) },
                    asType = {
                        createDeclaredType(
                            getQualifiedName = { it.qualifiedName!! },
                            asElement = {
                                it.asTypeElement(caches)
                            },
                            getTypeArguments = { emptyList() }
                        )
                    },
                )
            }
        },
        getSuperclass = {
            val superClass = this.superClassType ?: return@createTypeElement null
            if (superClass is PsiType) {
                superClass.asTypeMirror(caches)
            } else {
                null
            }
        }
    ).also {
        caches[this.qualifiedName!!] = it
    }
}

fun PsiMethod.asExecutableElement(caches: MutableMap<String, TypeElement>): ExecutableElement? {
    val containingClass = containingClass ?: return null
    return createExecutableElement(
        getKind = {
            if (isConstructor) {
                ElementKind.CONSTRUCTOR
            } else {
                ElementKind.METHOD
            }
        },
        getSimpleName = { createName(name) },
        getModifiers = { setOf(Modifier.PUBLIC) },
        getEnclosingElement = { containingClass.asTypeElement(caches) },
        getParameters = {
            parameterList.parameters.map { parameter ->
                createVariableElement(
                    getSimpleName = { createName(parameter.name) },
                    asType = { parameter.type.asTypeMirror(caches) }
                )
            }
        },
        getReturnType = {
            val returnType = returnType ?: throw IllegalStateException("Return type is null")
            returnType.asTypeMirror(caches)
        },
        getAnnotation = { anno ->
            modifierList.annotations.find { it.hasQualifiedName(anno.name) }
                ?.findAnnotation()
        },
        getAnnotationMirrors = {
            modifierList.annotations.mapNotNull {
                it.findAnnotation()?.let {
                    containingClass.createAnnotationMirror(
                        it,
                        caches
                    )
                }
            }
        },
        getAnnotationsByType = { anno ->
            modifierList.annotations.find { it.hasQualifiedName(anno.name) }
                ?.findAnnotation()?.let { arrayOf(it) }
        },
        isDefault = { body != null }
    )
}

fun PsiField.asVariableElement(caches: MutableMap<String, TypeElement>): VariableElement? {
    val containingClass = containingClass ?: return null
    return createVariableElement(
        getSimpleName = { createName(name) },
        getKind = { ElementKind.FIELD },
        getEnclosingElement = {
            containingClass.asTypeElement(caches)
        },
        asType = {
            type.asTypeMirror(caches)
        },
        getModifiers = { setOf(Modifier.PUBLIC) }
    )
}

fun PsiTypeParameter.asTypeMirror(caches: MutableMap<String, TypeElement>): TypeMirror {
    return createTypeVariable(
        asElement = {
            this.asTypeElement(caches)
        }
    )
}

fun PsiType.asTypeMirror(caches: MutableMap<String, TypeElement>): TypeMirror {
    val generic = PsiUtil.resolveGenericsClassInType(this)

    when (canonicalText.substringBefore("[")) {
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
    }?.also {
        return if (canonicalText.endsWith("]")) {
            createArrayType(
                getComponentType = {
                    it
                }
            )
        } else {
            it
        }
    }

    val genericElement =
        generic.element
            ?: throw IllegalStateException("The generic '${canonicalText}' element is null")

    return createDeclaredType(
        getQualifiedName = { genericElement.qualifiedName!! },
        asElement = {
            genericElement.asTypeElement(caches)
        },
        getTypeArguments = {
            if (generic.substitutor != PsiSubstitutor.EMPTY) {
                generic.element?.typeParameters?.mapNotNull {
                    val parameter =
                        generic.substitutor.substitute(it) ?: return@mapNotNull null
                    createDeclaredType(
                        getQualifiedName = { parameter.canonicalText },
                        asElement = {
                            caches[parameter.canonicalText]
                                ?: PsiUtil.resolveGenericsClassInType(parameter).element?.asTypeElement(
                                    caches
                                )
                                ?: throw IllegalStateException("Generic element is null")
                        },
                        getTypeArguments = { emptyList() },
                    )
                } ?: emptyList()
            } else {
                emptyList()
            }
        },
        getAnnotationMirrors = { emptyList() }
    )
}

data class Apt(
    val processingEnvironment: ProcessingEnvironment,
    val typeElements: Set<TypeElement>,
    val sources: List<Source>,
)

fun Project.psiClassesToApt(
    psiClasses: Set<PsiClass>
): Apt {
    val typeElementCaches = mutableMapOf<String, TypeElement>()

    psiClasses.forEach { psiClass ->
        val qualifiedName = psiClass.qualifiedName ?: return@forEach
        typeElementCaches[qualifiedName] = psiClass.asTypeElement(typeElementCaches)
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
                            Path(System.getProperty("user.dir"), "dummy.java").toUri(),
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
                            Path(System.getProperty("user.dir")).resolve(relativeName.toString()).toUri(),
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
                        return typeElementCaches[name.toString()] ?: JavaPsiFacade.getInstance(this@psiClassesToApt)
                            .findClass(name.toString(), this@psiClassesToApt.allScope())
                            ?.asTypeElement(typeElementCaches)
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
                        return typeElementCaches[t.toString()]
                            ?: JavaPsiFacade.getInstance(this@psiClassesToApt)
                                .findClass(t.toString(), this@psiClassesToApt.allScope())
                                ?.asTypeElement(typeElementCaches)
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
                            val t1Element = t1.asElement()?.toString() ?: t1.toString()
                            val t2Element = t2.asElement()?.toString() ?: t2.toString()

                            var eq =
                                t1Element.contentEquals(t2Element)

                            if (!eq) {
                                if (t2Element == "java.lang.Enum") {
                                    eq = typeElementCaches[t1Element]?.kind == ElementKind.ENUM
                                } else {
                                    val project = this@psiClassesToApt
                                    val psiClass =
                                        JavaPsiFacade.getInstance(project).findClass(t2Element, project.allScope())
                                            ?: return false
                                    eq = t1Element in ClassInheritorsSearch.search(psiClass, project.allScope(), true)
                                        .map { it.qualifiedName }
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

                            override fun toString(): String {
                                return typeElem?.qualifiedName.toString()
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

private fun PsiAnnotation.findAnnotation(): Annotation? = when (qualifiedName) {
    Immutable::class.qualifiedName -> Utility.immutable()
    Entity::class.qualifiedName -> Utility.entity()
    MappedSuperclass::class.qualifiedName -> Utility.mappedSuperclass()
    Embeddable::class.qualifiedName -> Utility.embeddable()
    ErrorFamily::class.qualifiedName -> Utility.errorFamily()
    ErrorField::class.qualifiedName -> Utility.errorField()
    Id::class.qualifiedName -> Utility.id()
    IdView::class.qualifiedName -> Utility.idView()
    Key::class.qualifiedName -> Utility.key()
    Version::class.qualifiedName -> Utility.version()
    Formula::class.qualifiedName -> Utility.formula()
    OneToOne::class.qualifiedName -> Utility.oneToOne()
    OneToMany::class.qualifiedName -> Utility.oneToMany()
    ManyToOne::class.qualifiedName -> Utility.manyToOne()
    ManyToMany::class.qualifiedName -> Utility.manyToMany()
    ManyToManyView::class.qualifiedName -> Utility.manyToManyView()
    Column::class.qualifiedName -> Utility.column()
    GeneratedValue::class.qualifiedName -> Utility.generatedValue()
    JoinColumn::class.qualifiedName -> Utility.joinColumn()
    JoinTable::class.qualifiedName -> Utility.joinTable()
    Transient::class.qualifiedName -> Utility._transient()
    Serialized::class.qualifiedName -> Utility.serialized()
    LogicalDeleted::class.qualifiedName -> Utility.logicalDeleted()
    Scalar::class.qualifiedName -> Utility.scalar()
    Nullable::class.qualifiedName -> Utility.nullable()
    org.jspecify.annotations.Nullable::class.qualifiedName -> Utility.jspecifyNullable()
    TypedTuple::class.qualifiedName -> Utility.typedTuple()
    JsonConverter::class.qualifiedName -> Utility.jsonConverter()
    else -> null
}?.let {
    ByteBuddy()
        .redefine(it.javaClass)
        .modifiers(Visibility.PUBLIC)
        .name("${it.javaClass.name}_Proxy")
        .method(ElementMatchers.namedOneOf(*it.javaClass.methods.filter { f ->
            Any::class.java.methods.any { it.name == f.name }.not() && f.name != "annotationType"
        }.map { it.name }.toTypedArray())).intercept(
            InvocationHandlerAdapter.of { proxy, method, args ->
                this@findAnnotation.findAttributeValue(method.name)?.toAny(method.returnType)
                    ?.arrayWrapper(method.returnType)
                    ?: method.invoke(it)
            }
        ).make().load(it.javaClass.classLoader).loaded.getDeclaredConstructor().also {
            it.isAccessible = true
        }.newInstance() as Annotation
} ?: run {
    val qualifiedName = qualifiedName?.takeIf { !it.startsWith("java.") } ?: return null
    val map = mutableMapOf<String, ByteArray>()

    class MyClassLoader : ClassLoader(this.javaClass.classLoader) {
        override fun loadClass(name: String, resolve: Boolean): Class<*> {
            return map[name]?.let {
                defineClass(name, it, 0, it.size)
            } ?: super.loadClass(name, resolve)
        }
    }

    val classLoader = MyClassLoader()
    val annotationUnloaded = ByteBuddy()
        .makeAnnotation()
        .modifiers(Visibility.PUBLIC)
        .name(qualifiedName)
        .make()
    map[qualifiedName] = annotationUnloaded.bytes

    val proxyName = qualifiedName + "_Proxy"
    val proxyAnnotationUnloaded = ByteBuddy()
        .subclass(Object::class.java)
        .modifiers(Visibility.PUBLIC)
        .name(proxyName)
        .implement(java.lang.annotation.Annotation::class.java)
        .method(named("annotationType"))
        .intercept(FixedValue.value(annotationUnloaded.typeDescription))

    map[proxyName] = proxyAnnotationUnloaded.make().bytes
    val forName = Class.forName(proxyName, true, classLoader)
    return forName.getDeclaredConstructor().also {
        it.isAccessible = true
    }.newInstance() as Annotation
}

fun PsiAnnotationMemberValue.toAny(returnType: Class<*>): Any? {
    return when (this) {
        is PsiLiteralExpression -> this.value.toString().let {
            when (returnType.kotlin) {
                Long::class -> it.toLong()
                Int::class -> it.toInt()
                Short::class -> it.toShort()
                Byte::class -> it.toByte()
                Boolean::class -> it.toBoolean()
                else -> it
            }
        }

        is PsiArrayInitializerMemberValue -> this.initializers.mapNotNull { it.toAny(returnType) }.let {
            when (returnType.name) {
                "[Ljava.lang.String;" -> run {
                    return it.map { it.toString() }.toTypedArray()
                }
            }
        }

        is PsiClassObjectAccessExpression -> try {
            try {
                Class.forName(this.type.canonicalText.subMiddle("<", ">"))
            } catch (_: Throwable) {
                null
            }
        } catch (_: Throwable) {
            null
        }

        else -> {
            null
        }
    }
}

fun Any.arrayWrapper(returnType: Class<*>): Any {
    return if (returnType.name.startsWith("[L")) {
        when (this) {
            is Long -> {
                arrayOf(this)
            }

            is Int -> {
                arrayOf(this)
            }

            is Short -> {
                arrayOf(this)
            }

            is Byte -> {
                arrayOf(this)
            }

            is Boolean -> {
                arrayOf(this)
            }

            is String -> {
                arrayOf(this)
            }

            else -> {
                this
            }
        }
    } else {
        this
    }
}

private fun createAnnotationValue(
    value: () -> Any? = { TODO("Not yet implemented") },
): AnnotationValue {
    return object : AnnotationValue {
        override fun getValue(): Any? {
            return try {
                value()?.let {
                    if (it is Class<*>) {
                        if (it == Void::class.java) {
                            "void"
                        } else {
                            it.name
                        }
                    } else {
                        it
                    }
                }
            } catch (_: Throwable) {
                null
            }
        }

        override fun <R : Any?, P : Any?> accept(
            v: AnnotationValueVisitor<R, P>?,
            p: P?
        ): R? {
            return v?.visit(this, p)
        }

        override fun toString(): String {
            return getValue().toString()
        }
    }
}

private fun PsiClass.createAnnotationMirror(
    anno: Annotation,
    caches: MutableMap<String, TypeElement>
): AnnotationMirror {
    return object : AnnotationMirror {
        override fun getAnnotationType(): DeclaredType {
            return createDeclaredType(
                getQualifiedName = { anno.annotationClass.qualifiedName!! },
                asElement = {
                    project.findPsiClass(anno.annotationClass.qualifiedName!!)?.asTypeElement(caches)
                        ?: createTypeElement(
                            getQualifiedName = { createName(anno.annotationClass.qualifiedName!!) },
                            getSimpleName = { createName(anno.annotationClass.simpleName!!) },
                            getEnclosingElement = {
                                createPackageElement(
                                    getQualifiedName = {
                                        createName(
                                            anno.annotationClass.qualifiedName!!.substringBeforeLast(
                                                "."
                                            )
                                        )
                                    }
                                )
                            }
                        )
                })
        }

        override fun getElementValues(): Map<out ExecutableElement, AnnotationValue> {
            return if (listOf(
                    Transient::class.qualifiedName,
                    JsonConverter::class.qualifiedName
                ).any { it == anno.annotationClass.qualifiedName }
            ) {
                anno.javaClass.methods.filter { f ->
                    Any::class.java.methods.any { it.name == f.name }.not() && f.name != "annotationType"
                }.mapNotNull { method ->
                    createExecutableElement(
                        getSimpleName = { createName(method.name) },
                    ) to createAnnotationValue {
                        anno.javaClass.getMethod(method.name).also {
                            it.isAccessible = true
                        }.invoke(anno)
                    }
                }.toMap()
            } else {
                emptyMap()
            }
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

private fun createArrayType(
    getComponentType: () -> TypeMirror,
    getAnnotationMirrors: () -> List<AnnotationMirror> = { emptyList() },
    getAnnotation: (Class<Annotation>) -> Annotation? = { null },
    getAnnotationsByType: (Class<Annotation>) -> Array<Annotation> = { emptyArray() },
): ArrayType {
    return object : ArrayType {
        override fun getComponentType(): TypeMirror {
            return getComponentType()
        }

        override fun getKind(): TypeKind {
            return TypeKind.ARRAY
        }

        override fun getAnnotationMirrors(): List<AnnotationMirror?>? {
            return getAnnotationMirrors()
        }

        override fun <A : Annotation?> getAnnotation(annotationType: Class<A?>?): A? {
            return getAnnotation.invoke(annotationType as Class<Annotation>) as A?
        }

        override fun <A : Annotation?> getAnnotationsByType(annotationType: Class<A>): Array<out A> {
            return getAnnotationsByType.invoke(annotationType as Class<Annotation>) as Array<out A>
        }

        override fun <R : Any?, P : Any?> accept(
            v: TypeVisitor<R, P>?,
            p: P?
        ): R? {
            return v?.visitArray(this, p)
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

        override fun equals(other: Any?): Boolean {
            if (other is PrimitiveType) {
                return getKind() == other.kind
            }
            return super.equals(other)
        }
    }
}

private fun createTypeVariable(
    asElement: () -> Element? = { TODO("Not yet implemented") },
    getUpperBound: () -> TypeMirror? = { TODO("Not yet implemented") },
    getLowerBound: () -> TypeMirror? = { TODO("Not yet implemented") },
    getKind: () -> TypeKind? = { TODO("Not yet implemented") },
): TypeVariable {
    return object : TypeVariable {
        override fun asElement(): Element? {
            return asElement()
        }

        override fun getUpperBound(): TypeMirror? {
            return getUpperBound()
        }

        override fun getLowerBound(): TypeMirror? {
            return getLowerBound()
        }

        override fun getKind(): TypeKind? {
            return getKind()
        }

        override fun getAnnotationMirrors(): List<AnnotationMirror?> {
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
            return v?.visitTypeVariable(this, p)
        }
    }
}

private fun createDeclaredType(
    getQualifiedName: () -> String,
    getKind: () -> TypeKind = { TypeKind.DECLARED },
    asElement: () -> Element = { TODO("${getQualifiedName()} Not yet implemented") },
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
    getTypeArguments: () -> List<TypeMirror> = { TODO("${getQualifiedName()} Not yet implemented") },
    getAnnotationMirrors: () -> List<AnnotationMirror> = { TODO("${getQualifiedName()} Not yet implemented") },
    getAnnotation: (Class<Annotation>) -> Annotation? = { TODO("${getQualifiedName()} Not yet implemented") },
    getAnnotationsByType: (Class<Annotation>) -> Array<Annotation> = { TODO("${getQualifiedName()} Not yet implemented") },
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

        override fun equals(other: Any?): Boolean {
            if (other is DeclaredType) {
                return this.toString() == other.toString()
            }
            return super.equals(other)
        }
    }
}

private fun createVariableElement(
    getSimpleName: () -> Name = { TODO("Not yet implemented") },
    asType: () -> TypeMirror = { TODO("${getSimpleName()} Not yet implemented") },
    getConstantValue: () -> Any? = { TODO("${getSimpleName()} Not yet implemented") },
    getEnclosingElement: () -> Element = { TODO("${getSimpleName()} Not yet implemented") },
    getKind: () -> ElementKind = { TODO("${getSimpleName()} Not yet implemented") },
    getModifiers: () -> Set<Modifier> = { TODO("${getSimpleName()} Not yet implemented") },
    getEnclosedElements: () -> List<Element> = { TODO("${getSimpleName()} Not yet implemented") },
    getAnnotationMirrors: () -> List<AnnotationMirror> = { TODO("${getSimpleName()} Not yet implemented") },
    getAnnotation: (Class<Annotation>) -> Annotation? = { TODO("${getSimpleName()} Not yet implemented") },
    getAnnotationsByType: (Class<Annotation>) -> Array<Annotation> = { TODO("${getSimpleName()} Not yet implemented") },
): VariableElement {
    return object : VariableElement {
        override fun asType(): TypeMirror? {
            return asType()
        }

        override fun getConstantValue(): Any? {
            return getConstantValue()
        }

        override fun getSimpleName(): Name? {
            return getSimpleName()
        }

        override fun getEnclosingElement(): Element? {
            return getEnclosingElement()
        }

        override fun getKind(): ElementKind? {
            return getKind()
        }

        override fun getModifiers(): Set<Modifier?>? {
            return getModifiers()
        }

        override fun getEnclosedElements(): List<Element?>? {
            return getEnclosedElements()
        }

        override fun getAnnotationMirrors(): List<AnnotationMirror?>? {
            return getAnnotationMirrors()
        }

        override fun <A : Annotation?> getAnnotation(annotationType: Class<A?>?): A? {
            return getAnnotation.invoke(annotationType as Class<Annotation>) as A?
        }

        override fun <A : Annotation?> getAnnotationsByType(annotationType: Class<A?>?): Array<out A?>? {
            return getAnnotationsByType.invoke(annotationType as Class<Annotation>) as Array<A?>?
        }

        override fun <R : Any?, P : Any?> accept(
            v: ElementVisitor<R?, P?>?,
            p: P?
        ): R? {
            return v?.visitVariable(this, p)
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
    getAnnotationsByType: (Class<Annotation>) -> Array<Annotation>? = { TODO("${getSimpleName()} Not yet implemented") },
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
    getSuperclass: () -> TypeMirror? = { null },
    getInterfaces: () -> List<TypeMirror> = { emptyList() },
    getTypeParameters: () -> List<TypeParameterElement> = { TODO("${getQualifiedName()} Not yet implemented") },
    getEnclosingElement: () -> Element = { TODO("${getQualifiedName()} Not yet implemented") },
    getKind: () -> ElementKind = { TODO("${getQualifiedName()} Not yet implemented") },
    getModifiers: () -> Set<Modifier> = { setOf(Modifier.PUBLIC) },
    getAnnotationMirrors: () -> List<AnnotationMirror> = { emptyList() },
    getAnnotation: (Class<Annotation>) -> Annotation? = { null },
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