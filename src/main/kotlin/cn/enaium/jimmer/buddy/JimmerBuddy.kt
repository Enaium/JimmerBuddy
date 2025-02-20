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

import cn.enaium.jimmer.buddy.utility.findProjects
import cn.enaium.jimmer.buddy.utility.psiClassesToApt
import com.intellij.compiler.CompilerConfiguration
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.IconLoader
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
import org.babyfish.jimmer.sql.Embeddable
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.MappedSuperclass
import java.io.File
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

    val allPsiClassCache = mutableMapOf<String, PsiClass>()
    val immutablePsiClassCache = mutableListOf<PsiClass>()

    var isInit = false

    fun init() {
        if (isInit) return
        val project = ProjectManager.getInstance().openProjects.takeIf { it.isNotEmpty() }?.first() ?: return
        val projects =
            findProjects(project.guessProjectDir()?.toNioPath()!!)

        val sourceFiles = projects.map {
            it.resolve("src").resolve("main").resolve("java").walk().filter { it.extension == "java" }
                .toList()
        }.flatten()
        sourcesProcess(project, sourceFiles)
        dtoProcess(project, projects.map {
            it.resolve("src").resolve("main").resolve("dto").walk().filter { it.extension == "dto" }
                .toList()
        }.flatten())
        isInit = true
    }

    fun initialize() {
        allPsiClassCache.clear()
        isInit = false
        init()
    }

    fun sourcesProcess(project: Project, sourceFiles: List<Path>) {
        ApplicationManager.getApplication().executeOnPooledThread {
            ApplicationManager.getApplication().runReadAction {
                DumbService.getInstance(project).runWhenSmart {
                    sourceFiles.forEach {
                        val psiFile = PsiFileFactory.getInstance(project)
                            .createFileFromText("dummy.java", JavaFileType.INSTANCE, it.readText())
                        immutablePsiClassCache.addAll(psiFile.children.mapNotNull { psi ->
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

                    val (pe, rootElements, sources) = psiClassesToApt(immutablePsiClassCache)
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
                                allPsiClassCache[(it as PsiClass).qualifiedName!!] = it
                            }
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun dtoProcess(project: Project, dtoFiles: List<Path>) {
        ApplicationManager.getApplication().executeOnPooledThread {
            ApplicationManager.getApplication().runReadAction {
                DumbService.getInstance(project).runWhenSmart {
                    val (pe, rootElements, sources) = psiClassesToApt(immutablePsiClassCache)
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
                                allPsiClassCache[(it as PsiClass).qualifiedName!!] = it
                            }
                        }
                    }
                }
            }
        }
    }

    fun isJimmerProject(project: Project): Boolean {

        if (project.isDisposed) {
            return false
        }

        ModuleManager.getInstance(project).modules.forEach { module ->
            val processorPath =
                CompilerConfiguration.getInstance(project).getAnnotationProcessingConfiguration(module).processorPath

            processorPath.split(File.pathSeparator).forEach { path ->
                if (path.contains("jimmer-apt")) {
                    return true
                }
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