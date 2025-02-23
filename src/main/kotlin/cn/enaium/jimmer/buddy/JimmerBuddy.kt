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

import cn.enaium.jimmer.buddy.utility.annotations
import cn.enaium.jimmer.buddy.utility.findProjects
import cn.enaium.jimmer.buddy.utility.ktClassToKsp
import cn.enaium.jimmer.buddy.utility.psiClassesToApt
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFileFactory
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.apt.client.DocMetadata
import org.babyfish.jimmer.apt.createContext
import org.babyfish.jimmer.apt.dto.AptDtoCompiler
import org.babyfish.jimmer.apt.dto.DtoGenerator
import org.babyfish.jimmer.apt.entry.EntryProcessor
import org.babyfish.jimmer.apt.error.ErrorProcessor
import org.babyfish.jimmer.apt.immutable.ImmutableProcessor
import org.babyfish.jimmer.dto.compiler.DtoFile
import org.babyfish.jimmer.dto.compiler.DtoModifier
import org.babyfish.jimmer.dto.compiler.OsFile
import org.babyfish.jimmer.error.ErrorFamily
import org.babyfish.jimmer.ksp.Context
import org.babyfish.jimmer.sql.Embeddable
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.MappedSuperclass
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtPsiFactory
import java.io.Reader
import java.nio.file.Path
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import kotlin.io.path.*


/**
 * @author Enaium
 */
object JimmerBuddy {

    const val NAME = "JimmerBuddy"
    val ICON = IconLoader.getIcon("/icons/logo.svg", JimmerBuddy::class.java)

    val javaAllPsiClassCache = mutableMapOf<String, PsiClass>()
    val javaImmutablePsiClassCache = mutableListOf<PsiClass>()

    val kotlinAllKtClassCache = mutableMapOf<String, KtClass>()
    val kotlinImmutableKtClassCache = mutableListOf<KtClass>()

    var isInit = false

    fun init() {
        if (isInit) return
        val project = ProjectManager.getInstance().openProjects.takeIf { it.isNotEmpty() }?.first() ?: return
        val projects =
            findProjects(project.guessProjectDir()?.toNioPath()!!)
        if (isJavaProject(project)) {
            sourcesProcessJava(project, projects.map {
                it.resolve("src").resolve("main").resolve("java").walk().filter { it.extension == "java" }.toList()
            }.flatten())
            dtoProcessJava(project, projects.map {
                it.resolve("src").resolve("main").resolve("dto").walk().filter { it.extension == "dto" }.toList()
            }.flatten())
        } else if (isKotlinProject(project)) {
            sourceProcessKotlin(project, projects.map {
                it.resolve("src").resolve("main").resolve("kotlin").walk().filter { it.extension == "kt" }.toList()
            }.flatten())
            dtoProcessKotlin(project, projects.map {
                it.resolve("src").resolve("main").resolve("dto").walk().filter { it.extension == "dto" }.toList()
            }.flatten())
        }
        isInit = true
    }

    fun initialize() {
        javaAllPsiClassCache.clear()
        isInit = false
        init()
    }

    fun sourcesProcessJava(project: Project, sourceFiles: List<Path>) {
        ApplicationManager.getApplication().executeOnPooledThread {
            ApplicationManager.getApplication().runReadAction {
                DumbService.getInstance(project).runWhenSmart {
                    sourceFiles.forEach {
                        val psiFile = PsiFileFactory.getInstance(project)
                            .createFileFromText("dummy.java", JavaFileType.INSTANCE, it.readText())
                        javaImmutablePsiClassCache.addAll(psiFile.children.mapNotNull { psi ->
                            return@mapNotNull if (psi is PsiClass) {
                                if (psi.modifierList?.annotations?.any { annotation ->
                                        annotation.hasQualifiedName(Immutable::class.qualifiedName!!)
                                                || annotation.hasQualifiedName(Entity::class.qualifiedName!!)
                                                || annotation.hasQualifiedName(MappedSuperclass::class.qualifiedName!!)
                                                || annotation.hasQualifiedName(Embeddable::class.qualifiedName!!)
                                                || annotation.hasQualifiedName(ErrorFamily::class.qualifiedName!!)
                                    } == false) return@mapNotNull null
                                psi
                            } else {
                                null
                            }
                        })
                    }

                    val (pe, rootElements, sources) = psiClassesToApt(javaImmutablePsiClassCache)
                    val context = createContext(
                        pe.elementUtils,
                        pe.typeUtils,
                        pe.filer,
                        false,
                        emptyArray(),
                        emptyArray(),
                        null,
                        null,
                        null,
                        null,
                        false
                    )
                    val roundEnv = createRoundEnvironment(rootElements)

                    try {
                        val immutableTypeElements = ImmutableProcessor(context, pe.messager).process(roundEnv).keys
                        EntryProcessor(context, immutableTypeElements).process()
                        ErrorProcessor(context, false).process(roundEnv)
                        sources.forEach {
                            val psiFile =
                                PsiFileFactory.getInstance(project)
                                    .createFileFromText("dummy.java", JavaFileType.INSTANCE, it)
                            psiFile.children.find { it is PsiClass }?.also {
                                javaAllPsiClassCache[(it as PsiClass).qualifiedName!!] = it
                            }
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun dtoProcessJava(project: Project, dtoFiles: List<Path>) {
        ApplicationManager.getApplication().executeOnPooledThread {
            ApplicationManager.getApplication().runReadAction {
                DumbService.getInstance(project).runWhenSmart {
                    val (pe, rootElements, sources) = psiClassesToApt(javaImmutablePsiClassCache)
                    val context = createContext(
                        pe.elementUtils,
                        pe.typeUtils,
                        pe.filer,
                        false,
                        emptyArray(),
                        emptyArray(),
                        null,
                        null,
                        null,
                        null,
                        false
                    )
                    dtoFiles.forEach {
                        val elements = pe.elementUtils
                        val dtoFile = DtoFile(object : OsFile {
                            override fun getAbsolutePath(): String {
                                return it.absolutePathString()
                            }

                            override fun openReader(): Reader {
                                return it.readText().reader()
                            }
                        }, "", "", emptyList(), it.name)
                        val compiler = AptDtoCompiler(dtoFile, elements, DtoModifier.STATIC)
                        val typeElement: TypeElement =
                            elements.getTypeElement(compiler.sourceTypeName) ?: return@forEach
                        val compile = compiler.compile(context.getImmutableType(typeElement))
                        compile.forEach {
                            DtoGenerator(context, DocMetadata(context), it).generate()
                        }
                        sources.forEach {
                            val psiFile =
                                PsiFileFactory.getInstance(project)
                                    .createFileFromText("dummy.java", JavaFileType.INSTANCE, it)
                            psiFile.children.find { it is PsiClass }?.also {
                                javaAllPsiClassCache[(it as PsiClass).qualifiedName!!] = it
                            }
                        }
                    }
                }
            }
        }
    }

    fun sourceProcessKotlin(project: Project, sourceFiles: List<Path>) {
        ApplicationManager.getApplication().executeOnPooledThread {
            ApplicationManager.getApplication().runReadAction {
                DumbService.getInstance(project).runWhenSmart {
                    sourceFiles.forEach {
                        val psiFile = VirtualFileManager.getInstance().findFileByNioPath(it)!!.toPsiFile(project)!!

                        kotlinImmutableKtClassCache.addAll(psiFile.children.mapNotNull { psi ->
                            return@mapNotNull if (psi is KtClass) {
                                if (psi.annotations().any { annotation ->
                                        val fqName = annotation?.fqName?.asString()
                                        fqName == Immutable::class.qualifiedName!!
                                                || fqName == Entity::class.qualifiedName!!
                                                || fqName == MappedSuperclass::class.qualifiedName!!
                                                || fqName == Embeddable::class.qualifiedName!!
                                                || fqName == ErrorFamily::class.qualifiedName!!
                                    } == false) return@mapNotNull null
                                psi
                            } else {
                                null
                            }
                        })
                    }
                    val (resolver, environment, sources) = ktClassToKsp(kotlinImmutableKtClassCache)
                    val context = Context(resolver, environment)
                    org.babyfish.jimmer.ksp.immutable.ImmutableProcessor(context, false).process()
                    org.babyfish.jimmer.ksp.error.ErrorProcessor(context, true).process()
                    sources.forEach {

                        val ktFile = KtPsiFactory(project).createFile(it)
                        ktFile.children.find { it is KtClass }?.also {
                            kotlinAllKtClassCache[(it as KtClass).fqName!!.asString()] = it
                        }
                    }
                }
            }
        }
    }

    fun dtoProcessKotlin(project: Project, dtoFiles: List<Path>) {

    }


    fun isJimmerProject(project: Project): Boolean {
        return isJavaProject(project) || isKotlinProject(project)
    }

    fun isJavaProject(project: Project): Boolean {
        if (project.isDisposed) {
            return false
        }

        val classesRoots = OrderEnumerator.orderEntries(project).runtimeOnly().classesRoots
        return classesRoots.any { it.name.startsWith("jimmer-core") } && classesRoots.none { it.name.startsWith("jimmer-core-kotlin") }
    }

    fun isKotlinProject(project: Project): Boolean {
        if (project.isDisposed) {
            return false
        }

        OrderEnumerator.orderEntries(project).runtimeOnly().classesRoots.forEach {
            if (it.name.startsWith("jimmer-core-kotlin")) {
                return true
            }
        }
        return false
    }


    private fun createRoundEnvironment(
        rootElements: Set<Element?>,
    ): RoundEnvironment {
        return object : RoundEnvironment {
            override fun processingOver(): Boolean {
                TODO("Not yet implemented")
            }

            override fun errorRaised(): Boolean {
                TODO("Not yet implemented")
            }

            override fun getRootElements(): Set<Element?> {
                return rootElements
            }

            override fun getElementsAnnotatedWith(a: TypeElement): Set<Element> {
                TODO("Not yet implemented")
            }

            override fun getElementsAnnotatedWith(a: Class<out Annotation?>?): Set<Element> {
                TODO("Not yet implemented")
            }
        }
    }
}