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

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import org.babyfish.jimmer.Formula
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.error.ErrorFamily
import org.babyfish.jimmer.sql.*
import org.jetbrains.kotlin.idea.base.utils.fqname.fqName
import org.jetbrains.kotlin.psi.KtClass
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import kotlin.reflect.KClass

/**
 * @author Enaium
 */
data class Ksp(
    val resolver: Resolver,
    val environment: SymbolProcessorEnvironment,
    val sources: List<String>,
)

fun ktClassToKsp(ktClasses: List<KtClass>): Ksp {
    val ksFiles = mutableListOf<KSFile>()
    val ksClassDeclarationCaches = mutableMapOf<String, KSClassDeclaration>()

    ktClasses.forEach { ktClass ->
        val fqName = ktClass.fqName!!.asString()
        ksClassDeclarationCaches[fqName] = createKSClassDeclaration(
            classKind = { ClassKind.INTERFACE },
            qualifiedName = { createKSName(fqName) },
            simpleName = { createKSName(ktClass.name!!) },
            packageName = { createKSName(fqName.substringBeforeLast(".")) },
            parentDeclaration = { null },
            annotations = {
                ktClass.annotations().mapNotNull { getAnnotation(it?.fqName?.asString()!!) }.asSequence()
            },
            asStarProjectedType = {
                createKSType(
                    declaration = { ksClassDeclarationCaches[fqName]!! }
                )
            }
        )
    }

    ktClasses.forEach { ktClass ->
        val fqName = ktClass.fqName!!.asString()
        val ksClassDeclarationCache = ksClassDeclarationCaches[fqName]!!

        ksClassDeclarationCaches[fqName] = createKSClassDeclaration(
            classKind = { ksClassDeclarationCache.classKind },
            qualifiedName = { ksClassDeclarationCache.qualifiedName },
            simpleName = { ksClassDeclarationCache.simpleName },
            packageName = { ksClassDeclarationCache.packageName },
            parentDeclaration = { ksClassDeclarationCache.parentDeclaration },
            annotations = { ksClassDeclarationCache.annotations },
            asStarProjectedType = { ksClassDeclarationCache.asStarProjectedType() },
            superTypes = {
                ktClass.superTypeListEntries.map {
                    createKSTypeReference(
                        resolve = {
                            createKSType(
                                declaration = {
                                    ksClassDeclarationCaches[it.typeReference?.type()?.fqName?.asString()]
                                        ?: throw IllegalArgumentException("Unknown super type ${it.typeReference?.type()?.fqName?.asString()}")
                                }
                            )
                        }
                    )
                }.asSequence()
            }
        )
    }

    ktClasses.forEach { ktClass ->
        val fqName = ktClass.fqName!!.asString()
        val ksClassDeclarationCache = ksClassDeclarationCaches[fqName]!!

        ksClassDeclarationCaches[fqName] = createKSClassDeclaration(
            classKind = { ksClassDeclarationCache.classKind },
            qualifiedName = { ksClassDeclarationCache.qualifiedName },
            simpleName = { ksClassDeclarationCache.simpleName },
            packageName = { ksClassDeclarationCache.packageName },
            parentDeclaration = { ksClassDeclarationCache.parentDeclaration },
            annotations = { ksClassDeclarationCache.annotations },
            asStarProjectedType = { ksClassDeclarationCache.asStarProjectedType() },
            superTypes = { ksClassDeclarationCache.superTypes },
            declarations = {
                ktClass.getProperties().mapNotNull { property ->
                    val typeReference = property.typeReference?.type() ?: return@mapNotNull null
                    val typeParameters = property.typeParameters

                    createKSPropertyDeclaration(
                        qualifiedName = {
                            createKSName(property.fqName!!.asString())
                        },
                        simpleName = {
                            createKSName(property.name!!)
                        },
                        annotations = {
                            property.annotations().mapNotNull { annotation ->
                                getAnnotation(annotation?.fqName?.asString()!!)
                            }.asSequence()
                        },
                        modifiers = { setOf(Modifier.ABSTRACT) },
                        type = {
                            createKSTypeReference(
                                resolve = {
                                    createKSType(
                                        arguments = {
                                            typeParameters.map { parameter ->
                                                createKSTypeArgument(
                                                    type = {
                                                        createKSTypeReference(
                                                            resolve = {
                                                                ksClassDeclarationCaches[parameter.fqName!!.asString()]?.asStarProjectedType()
                                                                    ?: throw IllegalArgumentException("Unknown type ${parameter.fqName!!.asString()}")
                                                            }
                                                        )
                                                    },
                                                    variance = { Variance.INVARIANT },
                                                )
                                            }
                                        },
                                        declaration = {
                                            ksClassDeclarationCaches[typeReference.fqName!!.asString()]
                                                ?: createKSClassDeclaration(
                                                    classKind = { ClassKind.CLASS },
                                                    qualifiedName = { createKSName(typeReference.fqName!!.asString()) },
                                                    simpleName = {
                                                        createKSName(
                                                            typeReference.fqName!!.shortName().asString()
                                                        )
                                                    },
                                                    packageName = { createKSName("") },
                                                    asStarProjectedType = {
                                                        createKSType()
                                                    },
                                                    annotations = { sequenceOf() }
                                                )
                                        },
                                        isMarkedNullable = { typeReference.isMarkedNullable },
                                    )
                                }
                            )
                        }
                    )
                }.asSequence()
            }
        )

        ksFiles.add(
            createKSFile(
                fileName = { ktClass.containingFile.name },
                filePath = { ktClass.containingFile.virtualFile.path },
                packageName = { ksClassDeclarationCaches[fqName]!!.packageName },
                declarations = { sequenceOf(ksClassDeclarationCaches[fqName]!!) },
                annotations = { sequenceOf() }
            )
        )
    }
    val sources = mutableListOf<String>()
    return Ksp(
        resolver = createResolver(newFiles = ksFiles.asSequence()),
        environment = SymbolProcessorEnvironment(
            emptyMap<String, String>(),
            KotlinVersion.CURRENT,
            object : CodeGenerator {
                override val generatedFile: Collection<File>
                    get() = TODO("Not yet implemented")

                override fun associate(
                    sources: List<KSFile>,
                    packageName: String,
                    fileName: String,
                    extensionName: String
                ) {
                    TODO("Not yet implemented")
                }

                override fun associateByPath(
                    sources: List<KSFile>,
                    path: String,
                    extensionName: String
                ) {
                    TODO("Not yet implemented")
                }

                override fun associateWithClasses(
                    classes: List<KSClassDeclaration>,
                    packageName: String,
                    fileName: String,
                    extensionName: String
                ) {
                    TODO("Not yet implemented")
                }

                override fun createNewFile(
                    dependencies: Dependencies,
                    packageName: String,
                    fileName: String,
                    extensionName: String
                ): OutputStream {
                    return object : ByteArrayOutputStream() {
                        override fun close() {
                            sources.add(toString())
                        }
                    }
                }

                override fun createNewFileByPath(
                    dependencies: Dependencies,
                    path: String,
                    extensionName: String
                ): OutputStream {
                    TODO("Not yet implemented")
                }
            },
            object : KSPLogger {
                override fun error(message: String, symbol: KSNode?) {
                    println(message)
                }

                override fun exception(e: Throwable) {
                }

                override fun info(message: String, symbol: KSNode?) {
                }

                override fun logging(message: String, symbol: KSNode?) {
                }

                override fun warn(message: String, symbol: KSNode?) {
                }
            }),
        sources
    )
}

private fun createResolver(
    allFiles: Sequence<KSFile> = emptySequence(),
    newFiles: Sequence<KSFile> = emptySequence(),
): Resolver {

    val collection = createKSClassDeclaration(asStarProjectedType = {
        createKSType(isAssignableFrom = {
            try {
                when (it.declaration.qualifiedName?.asString()?.substringBefore("<")) {
                    Collection::class.simpleName -> true
                    List::class.simpleName -> true
                    Set::class.simpleName -> true
                    MutableList::class.simpleName -> true
                    MutableSet::class.simpleName -> true
                    MutableCollection::class.simpleName -> true
                    else -> false
                }
            } catch (_: Throwable) {
                false
            }
        })
    })
    val list = createKSClassDeclaration(asStarProjectedType = {
        createKSType(isAssignableFrom = {
            try {
                when (it.declaration.qualifiedName?.asString()?.substringBefore("<")) {
                    List::class.simpleName -> true
                    MutableList::class.simpleName -> true
                    else -> false
                }
            } catch (_: Throwable) {
                false
            }
        })
    })
    val map = createKSClassDeclaration(asStarProjectedType = {
        createKSType(isAssignableFrom = {
            try {
                when (it.declaration.qualifiedName?.asString()?.substringBefore("<")) {
                    Map::class.simpleName -> true
                    MutableMap::class.simpleName -> true
                    else -> false
                }
            } catch (_: Throwable) {
                false
            }
        })
    })

    return object : Resolver {
        override val builtIns: KSBuiltIns
            get() = object : KSBuiltIns {
                override val annotationType: KSType
                    get() = TODO("Not yet implemented")
                override val anyType: KSType
                    get() = TODO("Not yet implemented")
                override val arrayType: KSType
                    get() = TODO("Not yet implemented")
                override val booleanType: KSType
                    get() = TODO("Not yet implemented")
                override val byteType: KSType
                    get() = TODO("Not yet implemented")
                override val charType: KSType
                    get() = TODO("Not yet implemented")
                override val doubleType: KSType
                    get() = TODO("Not yet implemented")
                override val floatType: KSType
                    get() = TODO("Not yet implemented")
                override val intType: KSType
                    get() = createKSType()
                override val iterableType: KSType
                    get() = TODO("Not yet implemented")
                override val longType: KSType
                    get() = TODO("Not yet implemented")
                override val nothingType: KSType
                    get() = TODO("Not yet implemented")
                override val numberType: KSType
                    get() = TODO("Not yet implemented")
                override val shortType: KSType
                    get() = TODO("Not yet implemented")
                override val stringType: KSType
                    get() = TODO("Not yet implemented")
                override val unitType: KSType
                    get() = TODO("Not yet implemented")

            }

        override fun createKSTypeReferenceFromKSType(type: KSType): KSTypeReference {
            TODO("Not yet implemented")
        }

        @KspExperimental
        override fun effectiveJavaModifiers(declaration: KSDeclaration): Set<Modifier> {
            TODO("Not yet implemented")
        }

        override fun getAllFiles(): Sequence<KSFile> {
            return allFiles
        }

        override fun getClassDeclarationByName(name: KSName): KSClassDeclaration? {
            return when (name.asString()) {
                "kotlin.collections.Collection" -> collection
                "kotlin.collections.List" -> list
                "kotlin.collections.Map" -> map
                else -> null
            }
        }

        @KspExperimental
        override fun getDeclarationsFromPackage(packageName: String): Sequence<KSDeclaration> {
            TODO("Not yet implemented")
        }

        @KspExperimental
        override fun getDeclarationsInSourceOrder(container: KSDeclarationContainer): Sequence<KSDeclaration> {
            TODO("Not yet implemented")
        }

        override fun getFunctionDeclarationsByName(
            name: KSName,
            includeTopLevel: Boolean
        ): Sequence<KSFunctionDeclaration> {
            TODO("Not yet implemented")
        }

        @KspExperimental
        override fun getJavaWildcard(reference: KSTypeReference): KSTypeReference {
            TODO("Not yet implemented")
        }

        @KspExperimental
        override fun getJvmCheckedException(function: KSFunctionDeclaration): Sequence<KSType> {
            TODO("Not yet implemented")
        }

        @KspExperimental
        override fun getJvmCheckedException(accessor: KSPropertyAccessor): Sequence<KSType> {
            TODO("Not yet implemented")
        }

        @KspExperimental
        override fun getJvmName(declaration: KSFunctionDeclaration): String? {
            TODO("Not yet implemented")
        }

        @KspExperimental
        override fun getJvmName(accessor: KSPropertyAccessor): String? {
            TODO("Not yet implemented")
        }

        override fun getKSNameFromString(name: String): KSName {
            return createKSName(name)
        }

        @KspExperimental
        override fun getModuleName(): KSName {
            TODO("Not yet implemented")
        }

        override fun getNewFiles(): Sequence<KSFile> {
            return newFiles
        }

        @KspExperimental
        override fun getOwnerJvmClassName(declaration: KSFunctionDeclaration): String? {
            TODO("Not yet implemented")
        }

        @KspExperimental
        override fun getOwnerJvmClassName(declaration: KSPropertyDeclaration): String? {
            TODO("Not yet implemented")
        }

        @KspExperimental
        override fun getPackageAnnotations(packageName: String): Sequence<KSAnnotation> {
            TODO("Not yet implemented")
        }

        @KspExperimental
        override fun getPackagesWithAnnotation(annotationName: String): Sequence<String> {
            TODO("Not yet implemented")
        }

        override fun getPropertyDeclarationByName(
            name: KSName,
            includeTopLevel: Boolean
        ): KSPropertyDeclaration? {
            TODO("Not yet implemented")
        }

        override fun getSymbolsWithAnnotation(
            annotationName: String,
            inDepth: Boolean
        ): Sequence<KSAnnotated> {
            TODO("Not yet implemented")
        }

        override fun getTypeArgument(
            typeRef: KSTypeReference,
            variance: Variance
        ): KSTypeArgument {
            TODO("Not yet implemented")
        }

        @KspExperimental
        override fun isJavaRawType(type: KSType): Boolean {
            TODO("Not yet implemented")
        }

        @KspExperimental
        override fun mapJavaNameToKotlin(javaName: KSName): KSName? {
            TODO("Not yet implemented")
        }

        @KspExperimental
        override fun mapKotlinNameToJava(kotlinName: KSName): KSName? {
            TODO("Not yet implemented")
        }

        @KspExperimental
        override fun mapToJvmSignature(declaration: KSDeclaration): String? {
            TODO("Not yet implemented")
        }

        override fun overrides(
            overrider: KSDeclaration,
            overridee: KSDeclaration
        ): Boolean {
            TODO("Not yet implemented")
        }

        override fun overrides(
            overrider: KSDeclaration,
            overridee: KSDeclaration,
            containingClass: KSClassDeclaration
        ): Boolean {
            TODO("Not yet implemented")
        }
    }
}

private fun getAnnotation(fqName: String): KSAnnotation? {
    return when (fqName) {
        Immutable::class.qualifiedName -> createAnnotation(Immutable::class)
        Entity::class.qualifiedName -> createAnnotation(Entity::class)
        MappedSuperclass::class.qualifiedName -> createAnnotation(MappedSuperclass::class)
        Embeddable::class.qualifiedName -> createAnnotation(Embeddable::class)
        ErrorFamily::class.qualifiedName -> createAnnotation(ErrorFamily::class)
        Id::class.qualifiedName -> createAnnotation(Id::class)
        IdView::class.qualifiedName -> createAnnotation(IdView::class)
        Key::class.qualifiedName -> createAnnotation(Key::class)
        Version::class.qualifiedName -> createAnnotation(Version::class)
        Formula::class.qualifiedName -> createAnnotation(Formula::class)
        OneToOne::class.qualifiedName -> createAnnotation(OneToOne::class)
        OneToMany::class.qualifiedName -> createAnnotation(OneToMany::class)
        ManyToOne::class.qualifiedName -> createAnnotation(ManyToOne::class)
        ManyToMany::class.qualifiedName -> createAnnotation(ManyToMany::class)
        Column::class.qualifiedName -> createAnnotation(Column::class)
        GeneratedValue::class.qualifiedName -> createAnnotation(GeneratedValue::class)
        JoinColumn::class.qualifiedName -> createAnnotation(JoinColumn::class)
        JoinTable::class.qualifiedName -> createAnnotation(JoinTable::class)
        Transient::class.qualifiedName -> createAnnotation(Transient::class)
        Serialized::class.qualifiedName -> createAnnotation(Serialized::class)
        LogicalDeleted::class.qualifiedName -> createAnnotation(LogicalDeleted::class)
        else -> null
    }
}

private fun createAnnotation(annotation: KClass<out Annotation>): KSAnnotation {
    return createKSAnnotation(
        annotationType = createKSTypeReference(
            resolve = {
                createKSType(
                    declaration = {
                        createKSClassDeclaration(
                            classKind = { ClassKind.ANNOTATION_CLASS },
                            qualifiedName = { createKSName(annotation.qualifiedName!!) },
                            simpleName = { createKSName(annotation.simpleName!!) },
                            packageName = { createKSName(annotation.java.packageName) },
                            annotations = { emptySequence() }
                        )
                    }
                )
            }
        ),
        shortName = {
            createKSName(annotation.simpleName!!)
        }
    )
}

private fun createKSName(name: String): KSName {
    return object : KSName {
        override fun asString(): String {
            return name
        }

        override fun getQualifier(): String {
            return name
        }

        override fun getShortName(): String {
            return name.substringAfter(".")
        }

        override fun toString(): String {
            return name
        }
    }
}

fun createKSFile(
    fileName: () -> String = { TODO("No yet implemented") },
    filePath: () -> String = { TODO("${fileName()} No yet implemented") },
    packageName: () -> KSName = { TODO("${fileName()} No yet implemented") },
    declarations: () -> Sequence<KSDeclaration> = { TODO("${fileName()} No yet implemented") },
    location: () -> Location = { TODO("${fileName()} No yet implemented") },
    origin: () -> Origin = { TODO("${fileName()} No yet implemented") },
    parent: () -> KSNode? = { null },
    annotations: () -> Sequence<KSAnnotation> = { TODO("${fileName()} No yet implemented") },
): KSFile {
    return object : KSFile {
        override val fileName: String
            get() = fileName()
        override val filePath: String
            get() = filePath()
        override val packageName: KSName
            get() = packageName()
        override val declarations: Sequence<KSDeclaration>
            get() = declarations()
        override val location: Location
            get() = location()
        override val origin: Origin
            get() = origin()
        override val parent: KSNode?
            get() = parent()

        override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
            return visitor.visitFile(this, data)
        }

        override val annotations: Sequence<KSAnnotation>
            get() = annotations()
    }
}

private fun createKSType(
    annotations: () -> Sequence<KSAnnotation> = { emptySequence() },
    arguments: () -> List<KSTypeArgument> = { emptyList() },
    declaration: () -> KSDeclaration = { TODO("Not yet implemented") },
    isError: () -> Boolean = { false },
    isFunctionType: () -> Boolean = { false },
    isMarkedNullable: () -> Boolean = { false },
    isSuspendFunctionType: () -> Boolean = { false },
    nullability: () -> Nullability = { Nullability.NOT_NULL },
    isAssignableFrom: (KSType) -> Boolean = { false },
    isCovarianceFlexible: () -> Boolean = { false },
    isMutabilityFlexible: () -> Boolean = { false },
    makeNotNullable: () -> KSType = { TODO("Not yet implemented") },
    makeNullable: () -> KSType = { TODO("Not yet implemented") },
    replace: (List<KSTypeArgument>) -> KSType = { TODO("Not yet implemented") },
    starProjection: () -> KSType = { TODO("Not yet implemented") },
): KSType {
    return object : KSType {
        override val annotations: Sequence<KSAnnotation>
            get() = annotations()
        override val arguments: List<KSTypeArgument>
            get() = arguments()
        override val declaration: KSDeclaration
            get() = declaration()
        override val isError: Boolean
            get() = isError()
        override val isFunctionType: Boolean
            get() = isFunctionType()
        override val isMarkedNullable: Boolean
            get() = isMarkedNullable()
        override val isSuspendFunctionType: Boolean
            get() = isSuspendFunctionType()
        override val nullability: Nullability
            get() = nullability()

        override fun isAssignableFrom(that: KSType): Boolean {
            return isAssignableFrom(that)
        }

        override fun isCovarianceFlexible(): Boolean {
            return isCovarianceFlexible()
        }

        override fun isMutabilityFlexible(): Boolean {
            return isMutabilityFlexible()
        }

        override fun makeNotNullable(): KSType {
            return makeNotNullable()
        }

        override fun makeNullable(): KSType {
            return makeNullable()
        }

        override fun replace(arguments: List<KSTypeArgument>): KSType {
            return replace(arguments)
        }

        override fun starProjection(): KSType {
            return starProjection()
        }
    }
}

private fun createKSClassDeclaration(
    qualifiedName: () -> KSName? = { TODO("Not yet implemented") },
    classKind: () -> ClassKind = { TODO("${qualifiedName()} Not yet implemented") },
    isCompanionObject: () -> Boolean = { TODO("${qualifiedName()} Not yet implemented") },
    primaryConstructor: () -> KSFunctionDeclaration? = { TODO("${qualifiedName()} Not yet implemented") },
    superTypes: () -> Sequence<KSTypeReference> = { TODO("${qualifiedName()} Not yet implemented") },
    asStarProjectedType: () -> KSType = { TODO("${qualifiedName()} Not yet implemented") },
    asType: (List<KSTypeArgument>) -> KSType = { TODO("${qualifiedName()} Not yet implemented") },
    getAllFunctions: () -> Sequence<KSFunctionDeclaration> = { TODO("${qualifiedName()} Not yet implemented") },
    getAllProperties: () -> Sequence<KSPropertyDeclaration> = { TODO("${qualifiedName()} Not yet implemented") },
    getSealedSubclasses: () -> Sequence<KSClassDeclaration> = { TODO("${qualifiedName()} Not yet implemented") },
    containingFile: () -> KSFile? = { TODO("${qualifiedName()} Not yet implemented") },
    docString: () -> String? = { TODO("${qualifiedName()} Not yet implemented") },
    packageName: () -> KSName = { TODO("${qualifiedName()} Not yet implemented") },
    parentDeclaration: () -> KSDeclaration? = { null },
    simpleName: () -> KSName = { TODO("${qualifiedName()} Not yet implemented") },
    typeParameters: () -> List<KSTypeParameter> = { emptyList() },
    modifiers: () -> Set<Modifier> = { setOf(Modifier.PUBLIC) },
    location: () -> Location = { TODO("${qualifiedName()} Not yet implemented") },
    origin: () -> Origin = { Origin.KOTLIN },
    parent: () -> KSNode? = { TODO("${qualifiedName()} Not yet implemented") },
    annotations: () -> Sequence<KSAnnotation> = { emptySequence() },
    isActual: () -> Boolean = { false },
    isExpect: () -> Boolean = { false },
    findActuals: () -> Sequence<KSDeclaration> = { emptySequence() },
    findExpects: () -> Sequence<KSDeclaration> = { emptySequence() },
    declarations: () -> Sequence<KSDeclaration> = { emptySequence() },
): KSClassDeclaration {
    return object : KSClassDeclaration {
        override val classKind: ClassKind
            get() = classKind()
        override val isCompanionObject: Boolean
            get() = isCompanionObject()
        override val primaryConstructor: KSFunctionDeclaration?
            get() = primaryConstructor()
        override val superTypes: Sequence<KSTypeReference>
            get() = superTypes()

        override fun asStarProjectedType(): KSType {
            return asStarProjectedType()
        }

        override fun asType(typeArguments: List<KSTypeArgument>): KSType {
            return asType(typeArguments)
        }

        override fun getAllFunctions(): Sequence<KSFunctionDeclaration> {
            return getAllFunctions()
        }

        override fun getAllProperties(): Sequence<KSPropertyDeclaration> {
            return getAllProperties()
        }

        override fun getSealedSubclasses(): Sequence<KSClassDeclaration> {
            return getSealedSubclasses()
        }

        override val containingFile: KSFile?
            get() = containingFile()
        override val docString: String?
            get() = docString()
        override val packageName: KSName
            get() = packageName()
        override val parentDeclaration: KSDeclaration?
            get() = parentDeclaration()
        override val qualifiedName: KSName?
            get() = qualifiedName()
        override val simpleName: KSName
            get() = simpleName()
        override val typeParameters: List<KSTypeParameter>
            get() = typeParameters()
        override val modifiers: Set<Modifier>
            get() = modifiers()
        override val location: Location
            get() = location()
        override val origin: Origin
            get() = origin()
        override val parent: KSNode?
            get() = parent()

        override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
            return visitor.visitClassDeclaration(this, data)
        }

        override val annotations: Sequence<KSAnnotation>
            get() = annotations()
        override val isActual: Boolean
            get() = isActual()
        override val isExpect: Boolean
            get() = isExpect()

        override fun findActuals(): Sequence<KSDeclaration> {
            return findActuals()
        }

        override fun findExpects(): Sequence<KSDeclaration> {
            return findExpects()
        }

        override val declarations: Sequence<KSDeclaration>
            get() = declarations()

        override fun toString(): String {
            return qualifiedName()?.asString() ?: simpleName().asString()
        }
    }
}

fun createKSPropertyDeclaration(
    qualifiedName: () -> KSName? = { TODO("Not yet implemented") },
    extensionReceiver: () -> KSTypeReference? = { TODO("${qualifiedName()} Not yet implemented") },
    getter: () -> KSPropertyGetter? = { null },
    hasBackingField: () -> Boolean = { false },
    isMutable: () -> Boolean = { false },
    setter: () -> KSPropertySetter? = { null },
    type: () -> KSTypeReference = { TODO("${qualifiedName()} Not yet implemented") },
    asMemberOf: (KSType) -> KSType = { TODO("${qualifiedName()} Not yet implemented") },
    findOverride: () -> KSPropertyDeclaration? = { TODO("${qualifiedName()} Not yet implemented") },
    isDelegated: () -> Boolean = { TODO("${qualifiedName()} Not yet implemented") },
    containingFile: () -> KSFile? = { TODO("${qualifiedName()} Not yet implemented") },
    docString: () -> String? = { null },
    packageName: () -> KSName = { TODO("${qualifiedName()} Not yet implemented") },
    parentDeclaration: () -> KSDeclaration? = { TODO("${qualifiedName()} Not yet implemented") },
    simpleName: () -> KSName = { TODO("${qualifiedName()} Not yet implemented") },
    typeParameters: () -> List<KSTypeParameter> = { TODO("${qualifiedName()} Not yet implemented") },
    modifiers: () -> Set<Modifier> = { TODO("${qualifiedName()} Not yet implemented") },
    location: () -> Location = { TODO("${qualifiedName()} Not yet implemented") },
    origin: () -> Origin = { TODO("${qualifiedName()} Not yet implemented") },
    parent: () -> KSNode? = { TODO("${qualifiedName()} Not yet implemented") },
    annotations: () -> Sequence<KSAnnotation> = { TODO("${qualifiedName()} Not yet implemented") },
    isActual: () -> Boolean = { TODO("${qualifiedName()} Not yet implemented") },
    isExpect: () -> Boolean = { TODO("${qualifiedName()} Not yet implemented") },
    findActuals: () -> Sequence<KSDeclaration> = { TODO("${qualifiedName()} Not yet implemented") },
    findExpects: () -> Sequence<KSDeclaration> = { TODO("${qualifiedName()} Not yet implemented") },
): KSPropertyDeclaration {
    return object : KSPropertyDeclaration {
        override val extensionReceiver: KSTypeReference?
            get() = extensionReceiver()
        override val getter: KSPropertyGetter?
            get() = getter()
        override val hasBackingField: Boolean
            get() = hasBackingField()
        override val isMutable: Boolean
            get() = isMutable()
        override val setter: KSPropertySetter?
            get() = setter()
        override val type: KSTypeReference
            get() = type()

        override fun asMemberOf(containing: KSType): KSType {
            return asMemberOf(containing)
        }

        override fun findOverridee(): KSPropertyDeclaration? {
            return findOverride()
        }

        override fun isDelegated(): Boolean {
            return isDelegated()
        }

        override val containingFile: KSFile?
            get() = containingFile()
        override val docString: String?
            get() = docString()
        override val packageName: KSName
            get() = packageName()
        override val parentDeclaration: KSDeclaration?
            get() = parentDeclaration()
        override val qualifiedName: KSName?
            get() = qualifiedName()
        override val simpleName: KSName
            get() = simpleName()
        override val typeParameters: List<KSTypeParameter>
            get() = typeParameters()
        override val modifiers: Set<Modifier>
            get() = modifiers()
        override val location: Location
            get() = location()
        override val origin: Origin
            get() = origin()
        override val parent: KSNode?
            get() = parent()

        override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
            return visitor.visitPropertyDeclaration(this, data)
        }

        override val annotations: Sequence<KSAnnotation>
            get() = annotations()
        override val isActual: Boolean
            get() = isActual()
        override val isExpect: Boolean
            get() = isExpect()

        override fun findActuals(): Sequence<KSDeclaration> {
            return findActuals()
        }

        override fun findExpects(): Sequence<KSDeclaration> {
            return findExpects()
        }
    }
}

private fun createKSTypeReference(
    element: () -> KSReferenceElement? = { null },
    resolve: () -> KSType = { TODO("Not yet implemented") },
    annotations: () -> Sequence<KSAnnotation> = { emptySequence() },
    location: () -> Location = { TODO("Not yet implemented") },
    origin: () -> Origin = { TODO("Not yet implemented") },
    parent: () -> KSNode? = { TODO("Not yet implemented") },
    modifiers: () -> Set<Modifier> = { TODO("Not yet implemented") },
): KSTypeReference {
    return object : KSTypeReference {
        override val element: KSReferenceElement?
            get() = element()

        override fun resolve(): KSType {
            return resolve()
        }

        override val annotations: Sequence<KSAnnotation>
            get() = annotations()
        override val location: Location
            get() = location()
        override val origin: Origin
            get() = origin()
        override val parent: KSNode?
            get() = parent()

        override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
            return visitor.visitTypeReference(this, data)
        }

        override val modifiers: Set<Modifier>
            get() = modifiers()
    }
}

private fun createKSAnnotation(
    annotationType: KSTypeReference,
    arguments: () -> List<KSValueArgument> = { emptyList() },
    defaultArguments: () -> List<KSValueArgument> = { emptyList() },
    shortName: () -> KSName = { TODO("Not yet implemented") },
    useSiteTarget: () -> AnnotationUseSiteTarget? = { TODO("Not yet implemented") },
    location: () -> Location = { TODO("Not yet implemented") },
    origin: () -> Origin = { TODO("Not yet implemented") },
    parent: () -> KSNode? = { TODO("Not yet implemented") },
): KSAnnotation {
    return object : KSAnnotation {
        override val annotationType: KSTypeReference
            get() = annotationType
        override val arguments: List<KSValueArgument>
            get() = arguments()
        override val defaultArguments: List<KSValueArgument>
            get() = defaultArguments()
        override val shortName: KSName
            get() = shortName()
        override val useSiteTarget: AnnotationUseSiteTarget?
            get() = useSiteTarget()
        override val location: Location
            get() = location()
        override val origin: Origin
            get() = origin()
        override val parent: KSNode?
            get() = parent()

        override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
            return visitor.visitAnnotation(this, data)
        }
    }
}

private fun createKSTypeArgument(
    type: () -> KSTypeReference = { TODO("Not yet implemented") },
    variance: () -> Variance = { TODO("Not yet implemented") },
    annotations: () -> Sequence<KSAnnotation> = { TODO("Not yet implemented") },
    location: () -> Location = { TODO("Not yet implemented") },
    origin: () -> Origin = { TODO("Not yet implemented") },
    parent: () -> KSNode? = { TODO("Not yet implemented") },
): KSTypeArgument {
    return object : KSTypeArgument {
        override val type: KSTypeReference
            get() = type()
        override val variance: Variance
            get() = variance()
        override val annotations: Sequence<KSAnnotation>
            get() = annotations()
        override val location: Location
            get() = location()
        override val origin: Origin
            get() = origin()
        override val parent: KSNode?
            get() = parent()

        override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
            return visitor.visitTypeArgument(this, data)
        }
    }
}