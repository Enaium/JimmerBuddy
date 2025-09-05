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

import cn.enaium.jimmer.buddy.service.PsiService
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.util.PsiUtil
import org.babyfish.jimmer.dto.compiler.DtoModifier
import org.babyfish.jimmer.ksp.Context
import org.babyfish.jimmer.ksp.JimmerProcessor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * @author Enaium
 */
typealias Cache = MutableMap<String, KSClassDeclaration>

fun PsiClass.asKSClassDeclaration(caches: Cache = mutableMapOf()): KSClassDeclaration {
    return caches[this.qualifiedName()] ?: createKSClassDeclaration(
        qualifiedName = { createKSName(this.qualifiedName()!!) },
        classKind = {
            if (this.isInterface) {
                ClassKind.INTERFACE
            } else if (this.isEnum) {
                ClassKind.ENUM_CLASS
            } else if (this.isAnnotationType) {
                ClassKind.ANNOTATION_CLASS
            } else {
                ClassKind.CLASS
            }
        },
        simpleName = { createKSName(this.name!!) },
        packageName = { createKSName(this.qualifiedName()!!.substringBeforeLast(".")) },
        superTypes = {
            this.superTypes.mapNotNull { superType ->
                val superClass = superType.resolve() ?: return@mapNotNull null
                createKSTypeReference(resolve = {
                    createKSType(
                        declaration = {
                            superClass.asKSClassDeclaration(caches)
                        },
                        arguments = {
                            superType.parameters.mapNotNull { parameterType ->
                                val klass =
                                    PsiUtil.resolveGenericsClassInType(parameterType).element ?: return@mapNotNull null
                                createKSTypeArgument(
                                    variance = {
                                        Variance.INVARIANT
                                    },
                                    type = {
                                        createKSTypeReference(
                                            resolve = {
                                                createKSType(
                                                    declaration = {
                                                        klass.asKSClassDeclaration(caches)
                                                    }
                                                )
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    )
                })
            }.asSequence()
        },
        parentDeclaration = { null },
        annotations = {
            this.annotations.mapNotNull { annotation ->
                val fqName =
                    annotation.qualifiedName?.takeIf { it.startsWith(jimmerAnnotationPrefix) }
                        ?: return@mapNotNull null
                createKSAnnotation(
                    annotationType =
                        createKSTypeReference(
                            resolve = {
                                createKSType(
                                    declaration = {
                                        project.findKtClass(fqName)?.asKSClassDeclaration(caches)
                                            ?: project.findPsiClass(fqName)?.asKSClassDeclaration(caches)
                                            ?: createKSClassDeclaration(
                                                classKind = { ClassKind.ANNOTATION_CLASS },
                                                qualifiedName = { createKSName(fqName) },
                                                simpleName = { createKSName(fqName.substringAfterLast(".")) },
                                                packageName = { createKSName(fqName.substringBeforeLast(".")) },
                                                annotations = { emptySequence() })
                                    })
                            }),
                    shortName = { createKSName(fqName.substringAfterLast(".")) },
                )
            }.asSequence()
        },
        declarations = { emptySequence() },
        asStarProjectedType = {
            createKSType(
                this.qualifiedName(),
                declaration = {
                    this@asKSClassDeclaration.asKSClassDeclaration(caches)
                },
                isAssignableFrom = {
                    it.toString() == qualifiedName()
                }
            )
        },
        asType = {
            this.asStarProjectedType()
        }
    ).also {
        caches[this.qualifiedName()!!] = it
    }
}

fun PsiService.Annotation.asKSAnnotation(caches: Cache = mutableMapOf()): KSAnnotation? {
    val fqName = this.fqName?.takeIf { it.startsWith(jimmerAnnotationPrefix) } ?: return null
    return createKSAnnotation(
        annotationType = createKSTypeReference(
            resolve = {
                createKSType(
                    declaration = {
                        project.findKtClass(fqName)?.asKSClassDeclaration(caches)
                            ?: project.findPsiClass(fqName)?.asKSClassDeclaration(caches)
                            ?: createKSClassDeclaration(
                                classKind = { ClassKind.ANNOTATION_CLASS },
                                qualifiedName = { createKSName(fqName) },
                                simpleName = { createKSName(fqName.substringAfterLast(".")) },
                                packageName = { createKSName(fqName.substringBeforeLast(".")) },
                                annotations = { emptySequence() })
                    })
            }), shortName = {
            createKSName(fqName.substringAfterLast("."))
        }, arguments = {
            this.arguments.map { argument ->
                createKSValueArgument(name = { createKSName(argument.name) }, value = { argument.value })
            }
        })
}

private fun PsiClass.qualifiedName(): String? {
    return when (this.qualifiedName) {
        java.lang.Long::class.java.name -> Long::class.qualifiedName
        Integer::class.java.name -> Int::class.qualifiedName
        java.lang.Short::class.java.name -> Short::class.qualifiedName
        java.lang.Byte::class.java.name -> Byte::class.qualifiedName
        java.lang.Double::class.java.name -> Double::class.qualifiedName
        java.lang.Float::class.java.name -> Float::class.qualifiedName
        String::class.java.name -> String::class.qualifiedName
        java.lang.Boolean::class.java.name -> Boolean::class.qualifiedName
        else -> this.qualifiedName
    }
}

fun KtClass.asKSClassDeclaration(caches: MutableMap<String, KSClassDeclaration> = mutableMapOf()): KSClassDeclaration {
    return caches[this.fqName!!.asString()] ?: createKSClassDeclaration(
        qualifiedName = { createKSName(fqName!!.asString()) },
        classKind = {
            if (isInterface()) {
                ClassKind.INTERFACE
            } else if (isEnum()) {
                ClassKind.ENUM_CLASS
            } else if (isAnnotation()) {
                ClassKind.ANNOTATION_CLASS
            } else {
                ClassKind.CLASS
            }
        },
        simpleName = { createKSName(name!!) },
        superTypes = {
            superTypeListEntries.mapNotNull {
                val superClass = it.typeReference?.type()?.ktClass ?: return@mapNotNull null
                createKSTypeReference(resolve = {
                    createKSType(declaration = {
                        superClass.asKSClassDeclaration(caches)
                    })
                })
            }.asSequence()
        },
        packageName = { createKSName(fqName!!.asString().substringBeforeLast(".")) },
        parentDeclaration = { null },
        annotations = {
            this.annotations().mapNotNull { annotation -> annotation.asKSAnnotation(caches) }.asSequence()
        },
        declarations = {
            if (this.isInterface()) {
                this.getProperties().mapNotNull { property ->
                    val typeReference = property.typeReference?.type() ?: return@mapNotNull null
                    val typeReferenceClass = typeReference.ktClass
                    val fqName = typeReference.fqName ?: return@mapNotNull null
                    createKSPropertyDeclaration(
                        qualifiedName = {
                            createKSName(property.fqName!!.asString())
                        },
                        simpleName = {
                            createKSName(property.name!!)
                        },
                        annotations = {
                            property.annotations().mapNotNull { annotation -> annotation.asKSAnnotation(caches) }
                                .asSequence()
                        },
                        modifiers = {
                            if (property.getter == null && property.setter == null) {
                                setOf(Modifier.ABSTRACT)
                            } else {
                                setOf()
                            }
                        },
                        type = {
                            createKSTypeReference(
                                resolve = {
                                    createKSType(
                                        arguments = {
                                            typeReference.arguments.mapNotNull { argument ->
                                                val fqName = argument.fqName ?: return@mapNotNull null
                                                val ktClass = argument.ktClass
                                                createKSTypeArgument(
                                                    type = {
                                                        createKSTypeReference(
                                                            resolve = {
                                                                createKSType(
                                                                    declaration = {
                                                                        ktClass?.asKSClassDeclaration(caches)
                                                                            ?: createKSClassDeclaration(
                                                                                classKind = { ClassKind.CLASS },
                                                                                qualifiedName = {
                                                                                    createKSName(fqName)
                                                                                },
                                                                                simpleName = {
                                                                                    createKSName(
                                                                                        fqName.substringAfterLast(
                                                                                            "."
                                                                                        )
                                                                                    )
                                                                                },
                                                                                packageName = {
                                                                                    createKSName(
                                                                                        fqName.substringBeforeLast(
                                                                                            "."
                                                                                        )
                                                                                    )
                                                                                },
                                                                                asStarProjectedType = {
                                                                                    this@createKSType
                                                                                })
                                                                    })
                                                            })
                                                    },
                                                    variance = { Variance.INVARIANT },
                                                )
                                            }
                                        },
                                        declaration = {
                                            typeReferenceClass?.asKSClassDeclaration(caches)
                                                ?: createKSClassDeclaration(
                                                    classKind = { ClassKind.CLASS },
                                                    qualifiedName = { createKSName(fqName) },
                                                    simpleName = {
                                                        createKSName(
                                                            fqName.substringAfterLast(".")
                                                        )
                                                    },
                                                    packageName = {
                                                        createKSName(
                                                            fqName.substringBeforeLast(".")
                                                        )
                                                    },
                                                    asStarProjectedType = {
                                                        this@createKSType
                                                    },
                                                    annotations = { sequenceOf() })
                                        },
                                        isMarkedNullable = { typeReference.nullable },
                                    )
                                })
                        },
                        getter = {
                            property.getter?.let {
                                createKSPropertyGetter(
                                    modifiers = { setOf(Modifier.FUN) },
                                )
                            }
                        },
                        parentDeclaration = { caches[this@asKSClassDeclaration.fqName!!.asString()] },
                    )
                }.asSequence()
            } else if (this.isEnum()) {
                this.getChildOfType<KtClassBody>()?.getChildrenOfType<KtEnumEntry>()?.map {
                    createKSClassDeclaration(
                        qualifiedName = { createKSName(it.name!!) },
                        classKind = { ClassKind.ENUM_ENTRY },
                        simpleName = { createKSName(it.name!!) },
                        parentDeclaration = { caches[fqName!!.asString()] },
                    )
                }?.asSequence() ?: emptySequence()
            } else {
                emptySequence()
            }
        },
        asStarProjectedType = {
            createKSType(
                this.fqName!!.asString(),
                declaration = {
                    this@asKSClassDeclaration.asKSClassDeclaration(caches)
                },
                isAssignableFrom = {
                    it.toString() == fqName!!.asString()
                }
            )
        },
        asType = {
            this.asStarProjectedType()
        },
        typeParameters = {
            this.typeParameters.map {
                createKSTypeParameter(
                    name = {
                        createKSName(it.name!!)
                    },
                )
            }
        }
    ).also {
        caches[this.fqName!!.asString()] = it
    }
}

data class Ksp(
    val resolver: Resolver,
    val environment: SymbolProcessorEnvironment,
    val sources: List<Source>,
)

fun Project.ktClassesToKsp(
    ktClasses: Set<KtClass>
): Ksp {
    val ksFiles = mutableListOf<KSFile>()
    val ksClassDeclarationCaches = mutableMapOf<String, KSClassDeclaration>()

    ktClasses.forEach { ktClass ->
        val fqName = ktClass.fqName?.asString() ?: return@forEach
        ksClassDeclarationCaches[fqName] =
            ktClass.asKSClassDeclaration(ksClassDeclarationCaches)

        ksFiles.add(
            createKSFile(
                fileName = { ktClass.containingFile.name },
                filePath = { ktClass.containingFile.virtualFile.path },
                packageName = { ksClassDeclarationCaches[fqName]!!.packageName },
                declarations = { sequenceOf(ksClassDeclarationCaches[fqName]!!) },
                annotations = { sequenceOf() }
            ))
    }
    val sources = mutableListOf<Source>()
    return Ksp(
        resolver = createResolver(
            caches = ksClassDeclarationCaches, newFiles = ksFiles.asSequence()
        ),
        environment = SymbolProcessorEnvironment(
            mapOf("jimmer.buddy.ignoreResourceGeneration" to "true"),
            KotlinVersion.CURRENT,
            object : CodeGenerator {
                override val generatedFile: Collection<File>
                    get() = TODO("Not yet implemented")

                override fun associate(
                    sources: List<KSFile>, packageName: String, fileName: String, extensionName: String
                ) {
                    TODO("Not yet implemented")
                }

                override fun associateByPath(
                    sources: List<KSFile>, path: String, extensionName: String
                ) {
                    TODO("Not yet implemented")
                }

                override fun associateWithClasses(
                    classes: List<KSClassDeclaration>, packageName: String, fileName: String, extensionName: String
                ) {
                    TODO("Not yet implemented")
                }

                override fun createNewFile(
                    dependencies: Dependencies, packageName: String, fileName: String, extensionName: String
                ): OutputStream {
                    return object : ByteArrayOutputStream() {
                        override fun close() {
                            sources.add(
                                Source(
                                    packageName = packageName,
                                    fileName = fileName,
                                    extensionName = extensionName,
                                    content = toString(Charsets.UTF_8)
                                )
                            )
                        }
                    }
                }

                override fun createNewFileByPath(
                    dependencies: Dependencies, path: String, extensionName: String
                ): OutputStream {
                    TODO("Not yet implemented")
                }
            },
            object : KSPLogger {
                override fun error(message: String, symbol: KSNode?) {
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

private fun Project.createResolver(
    caches: Map<String, KSClassDeclaration>,
    newFiles: Sequence<KSFile> = emptySequence(),
): Resolver {

    val collection = createKSClassDeclaration(asStarProjectedType = {
        createKSType(isAssignableFrom = {
            when (it.declaration.qualifiedName?.asString()?.substringBefore("<")) {
                Collection::class.qualifiedName -> true
                List::class.qualifiedName -> true
                Set::class.qualifiedName -> true
                MutableList::class.qualifiedName -> true
                MutableSet::class.qualifiedName -> true
                MutableCollection::class.qualifiedName -> true
                else -> false
            }
        })
    })
    val list = createKSClassDeclaration(asStarProjectedType = {
        createKSType(isAssignableFrom = {
            when (it.declaration.qualifiedName?.asString()?.substringBefore("<")) {
                List::class.qualifiedName -> true
                MutableList::class.qualifiedName -> true
                else -> false
            }
        })
    })
    val map = createKSClassDeclaration(asStarProjectedType = {
        createKSType(isAssignableFrom = {
            try {
                when (it.declaration.qualifiedName?.asString()?.substringBefore("<")) {
                    Map::class.qualifiedName -> true
                    MutableMap::class.qualifiedName -> true
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
            return emptySequence()
        }

        override fun getClassDeclarationByName(name: KSName): KSClassDeclaration? {
            return when (name.asString()) {
                "kotlin.collections.Collection" -> collection
                "kotlin.collections.List" -> list
                "kotlin.collections.Map" -> map
                else -> caches[name.asString()]
            } ?: run {
                var cd =
                    (findKtClass(name.asString()))?.asKSClassDeclaration(caches.toMutableMap())

                if (cd == null) {
                    cd = findPsiClass(name.asString())?.asKSClassDeclaration(caches.toMutableMap())
                }

                cd
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
            name: KSName, includeTopLevel: Boolean
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
            name: KSName, includeTopLevel: Boolean
        ): KSPropertyDeclaration? {
            TODO("Not yet implemented")
        }

        override fun getSymbolsWithAnnotation(
            annotationName: String, inDepth: Boolean
        ): Sequence<KSAnnotated> {
            TODO("Not yet implemented")
        }

        override fun getTypeArgument(
            typeRef: KSTypeReference, variance: Variance
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
            overrider: KSDeclaration, overridee: KSDeclaration
        ): Boolean {
            TODO("Not yet implemented")
        }

        override fun overrides(
            overrider: KSDeclaration, overridee: KSDeclaration, containingClass: KSClassDeclaration
        ): Boolean {
            TODO("Not yet implemented")
        }
    }
}

fun createKSName(name: String): KSName {
    return object : KSName {
        override fun asString(): String {
            return name
        }

        override fun getQualifier(): String {
            return name
        }

        override fun getShortName(): String {
            return name.substringAfterLast(".")
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

fun createKSType(
    qualifiedName: String? = null,
    annotations: () -> Sequence<KSAnnotation> = { emptySequence() },
    arguments: () -> List<KSTypeArgument> = { emptyList() },
    declaration: KSType.() -> KSDeclaration = { TODO("Not yet implemented") },
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
            get() = declaration.invoke(this)
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

        override fun toString(): String {
            return qualifiedName ?: super.toString()
        }
    }
}

fun createKSClassDeclaration(
    qualifiedName: () -> KSName? = { TODO("Not yet implemented") },
    classKind: () -> ClassKind = { ClassKind.CLASS },
    isCompanionObject: () -> Boolean = { TODO("${qualifiedName()} Not yet implemented") },
    primaryConstructor: () -> KSFunctionDeclaration? = { TODO("${qualifiedName()} Not yet implemented") },
    superTypes: () -> Sequence<KSTypeReference> = { TODO("${qualifiedName()} Not yet implemented") },
    asStarProjectedType: () -> KSType = { TODO("${qualifiedName()} Not yet implemented") },
    asType: KSClassDeclaration.(List<KSTypeArgument>) -> KSType = { TODO("${qualifiedName()} Not yet implemented") },
    getAllFunctions: () -> Sequence<KSFunctionDeclaration> = { TODO("${qualifiedName()} Not yet implemented") },
    getAllProperties: () -> Sequence<KSPropertyDeclaration> = { TODO("${qualifiedName()} Not yet implemented") },
    getSealedSubclasses: () -> Sequence<KSClassDeclaration> = { TODO("${qualifiedName()} Not yet implemented") },
    containingFile: () -> KSFile? = { TODO("${qualifiedName()} Not yet implemented") },
    docString: () -> String? = { null },
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
            return asType.invoke(this, typeArguments)
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

fun createKSPropertyGetter(
    returnType: () -> KSTypeReference? = { null },
    receiver: () -> KSPropertyDeclaration = { TODO("Not yet implemented") },
    declarations: () -> Sequence<KSDeclaration> = { emptySequence() },
    location: () -> Location = { TODO("Not yet implemented") },
    origin: () -> Origin = { Origin.KOTLIN },
    parent: () -> KSDeclaration? = { null },
    annotations: () -> Sequence<KSAnnotation> = { emptySequence() },
    modifiers: () -> Set<Modifier> = { emptySet() },
): KSPropertyGetter {
    return object : KSPropertyGetter {
        override val returnType: KSTypeReference?
            get() = returnType()
        override val receiver: KSPropertyDeclaration
            get() = receiver()
        override val declarations: Sequence<KSDeclaration>
            get() = declarations()
        override val location: Location
            get() = location()
        override val origin: Origin
            get() = origin()
        override val parent: KSNode?
            get() = parent()

        override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
            return visitor.visitPropertyGetter(this, data)
        }

        override val annotations: Sequence<KSAnnotation>
            get() = annotations()
        override val modifiers: Set<Modifier>
            get() = modifiers()
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
    parentDeclaration: () -> KSDeclaration? = { null },
    simpleName: () -> KSName = { TODO("${qualifiedName()} Not yet implemented") },
    typeParameters: () -> List<KSTypeParameter> = { TODO("${qualifiedName()} Not yet implemented") },
    modifiers: () -> Set<Modifier> = { TODO("${qualifiedName()} Not yet implemented") },
    location: () -> Location = { TODO("${qualifiedName()} Not yet implemented") },
    origin: () -> Origin = { TODO("${qualifiedName()} Not yet implemented") },
    parent: () -> KSNode? = { null },
    annotations: () -> Sequence<KSAnnotation> = { TODO("${qualifiedName()} Not yet implemented") },
    isActual: () -> Boolean = { false },
    isExpect: () -> Boolean = { false },
    findActuals: () -> Sequence<KSDeclaration> = { emptySequence() },
    findExpects: () -> Sequence<KSDeclaration> = { emptySequence() },
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

private fun createKSValueArgument(
    isSpread: () -> Boolean = { false },
    name: () -> KSName? = { null },
    value: () -> Any? = { null },
    annotations: () -> Sequence<KSAnnotation> = { emptySequence() },
    location: () -> Location = { TODO("Not yet implemented") },
    origin: () -> Origin = { TODO("Not yet implemented") },
    parent: () -> KSNode? = { null },
): KSValueArgument {
    return object : KSValueArgument {
        override val isSpread: Boolean
            get() = isSpread()
        override val name: KSName?
            get() = name()
        override val value: Any?
            get() = value()
        override val annotations: Sequence<KSAnnotation>
            get() = annotations()
        override val location: Location
            get() = location()
        override val origin: Origin
            get() = origin()
        override val parent: KSNode?
            get() = parent()

        override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
            return visitor.visitValueArgument(this, data)
        }
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
    type: () -> KSTypeReference? = { TODO("Not yet implemented") },
    variance: () -> Variance = { TODO("Not yet implemented") },
    annotations: () -> Sequence<KSAnnotation> = { TODO("Not yet implemented") },
    location: () -> Location = { TODO("Not yet implemented") },
    origin: () -> Origin = { TODO("Not yet implemented") },
    parent: () -> KSNode? = { TODO("Not yet implemented") },
): KSTypeArgument {
    return object : KSTypeArgument {
        override val type: KSTypeReference?
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

private fun createKSTypeParameter(
    bounds: () -> Sequence<KSTypeReference> = { TODO("Not yet implemented") },
    isReified: () -> Boolean = { TODO("Not yet implemented") },
    name: () -> KSName,
    variance: () -> Variance = { TODO("Not yet implemented") },
    containingFile: () -> KSFile? = { TODO("Not yet implemented") },
    docString: () -> String? = { TODO("Not yet implemented") },
    packageName: () -> KSName = { TODO("Not yet implemented") },
    parentDeclaration: () -> KSDeclaration? = { TODO("Not yet implemented") },
    qualifiedName: () -> KSName? = { TODO("Not yet implemented") },
    simpleName: () -> KSName = { TODO("Not yet implemented") },
    typeParameters: () -> List<KSTypeParameter> = { TODO("Not yet implemented") },
    modifiers: () -> Set<Modifier> = { TODO("Not yet implemented") },
    location: () -> Location = { TODO("Not yet implemented") },
    origin: () -> Origin = { TODO("Not yet implemented") },
    parent: () -> KSNode? = { TODO("Not yet implemented") },
    annotations: () -> Sequence<KSAnnotation> = { TODO("Not yet implemented") },
    isActual: () -> Boolean = { TODO("Not yet implemented") },
    isExpect: () -> Boolean = { TODO("Not yet implemented") },
    findActuals: () -> Sequence<KSDeclaration> = { emptySequence() },
    findExpects: () -> Sequence<KSDeclaration> = { emptySequence() },
): KSTypeParameter {
    return object : KSTypeParameter {
        override val bounds: Sequence<KSTypeReference>
            get() = bounds()
        override val isReified: Boolean
            get() = isReified()
        override val name: KSName
            get() = name()
        override val variance: Variance
            get() = variance()
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
            return visitor.visitTypeParameter(this, data)
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

@Suppress("UNCHECKED_CAST")
fun createKspOption(
    options: Map<String, String>, context: Context, codeGenerator: CodeGenerator
): KspOption {
    val jimmerProcessor =
        JimmerProcessor(SymbolProcessorEnvironment(options, KotlinVersion.CURRENT, codeGenerator, object : KSPLogger {
            override fun error(message: String, symbol: KSNode?) {
            }

            override fun exception(e: Throwable) {
            }

            override fun info(message: String, symbol: KSNode?) {
            }

            override fun logging(message: String, symbol: KSNode?) {
            }

            override fun warn(message: String, symbol: KSNode?) {
            }
        }))

    val jimmerProcessorClass = JimmerProcessor::class
    return KspOption(
        context,
        jimmerProcessorClass.declaredMemberProperties.find { it.name == "isModuleRequired" }!!
            .also { it.isAccessible = true }.get(jimmerProcessor) as Boolean,
        jimmerProcessorClass.declaredMemberProperties.find { it.name == "dtoDirs" }!!.also { it.isAccessible = true }
            .get(jimmerProcessor) as Collection<String>,
        jimmerProcessorClass.declaredMemberProperties.find { it.name == "dtoTestDirs" }!!
            .also { it.isAccessible = true }.get(jimmerProcessor) as Collection<String>,
        jimmerProcessorClass.declaredMemberProperties.find { it.name == "defaultNullableInputModifier" }!!
            .also { it.isAccessible = true }.get(jimmerProcessor) as DtoModifier,
        jimmerProcessorClass.declaredMemberProperties.find { it.name == "checkedException" }!!
            .also { it.isAccessible = true }.get(jimmerProcessor) as Boolean,
        jimmerProcessorClass.declaredMemberProperties.find { it.name == "dtoMutable" }!!.also { it.isAccessible = true }
            .get(jimmerProcessor) as Boolean,
        jimmerProcessorClass.declaredMemberProperties.find { it.name == "excludedUserAnnotationPrefixes" }!!
            .also { it.isAccessible = true }.get(jimmerProcessor) as List<String>
    )
}

data class KspOption(
    val context: Context,
    val isModuleRequired: Boolean,
    val dtoDirs: Collection<String>,
    val dtoTestDirs: Collection<String>,
    val defaultNullableInputModifier: DtoModifier,
    val checkedException: Boolean,
    val mutable: Boolean,
    val excludedUserAnnotationPrefixes: List<String>
)