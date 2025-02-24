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

import cn.enaium.jimmer.buddy.utility.*
import com.google.devtools.ksp.getClassDeclarationByName
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
import org.babyfish.jimmer.ksp.KspDtoCompiler
import org.babyfish.jimmer.sql.Embeddable
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.MappedSuperclass
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClass
import java.io.Reader
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
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

    val javaImmutablePsiClassCache = CopyOnWriteArrayList<PsiClass>()

    val kotlinImmutableKtClassCache = CopyOnWriteArrayList<KtClass>()

    var isInit = false

    val PSI_SHARED: PsiShared = ServiceLoader.load(PsiShared::class.java, JimmerBuddy::class.java.classLoader).first()

    fun init() {
        if (isInit) return
        if (DumbService.isDumb(ProjectManager.getInstance().openProjects.firstOrNull() ?: return)) {
            return
        }

        isInit = true
        val project = ProjectManager.getInstance().openProjects.takeIf { it.isNotEmpty() }?.first() ?: return
        val projects =
            findProjects(project.guessProjectDir()?.toNioPath()!!)
        if (isJavaProject(project)) {
            sourcesProcessJava(project, projects.map {
                it to it.resolve("src/main/java").walk().filter { it.extension == "java" }
                    .toList()
            }.associate { it })
            dtoProcessJava(project, projects.map {
                it.resolve("src/main/dto").walk().filter { it.extension == "dto" }.toList()
            }.flatten())
        } else if (isKotlinProject(project)) {
            sourceProcessKotlin(project, projects.map {
                it to it.resolve("src/main/kotlin").walk().filter { it.extension == "kt" }
                    .toList()
            }.associate { it })
            dtoProcessKotlin(
                project,
                projects.map { it.resolve("src/main/dto").walk().filter { it.extension == "dto" }.toList() }.flatten()
            )
        }
    }

    fun initialize() {
        javaImmutablePsiClassCache.clear()
        kotlinImmutableKtClassCache.clear()
        isInit = false
        init()
    }

    fun sourcesProcessJava(project: Project, projects: Map<Path, List<Path>>) {
        ApplicationManager.getApplication().executeOnPooledThread {
            ApplicationManager.getApplication().runReadAction {
                if (!DumbService.isDumb(project)) {
                    projects.forEach { (projectDir, sourceFiles) ->
                        val psiCaches = if (projects.size == 1) javaImmutablePsiClassCache else CopyOnWriteArrayList()
                        sourceFiles.forEach {
                            val psiFile = PsiFileFactory.getInstance(project)
                                .createFileFromText("dummy.java", JavaFileType.INSTANCE, it.readText())
                            psiCaches.addAll(psiFile.children.mapNotNull { psi ->
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

                        val generatedDir = getGeneratedDir(project, projectDir) ?: return@forEach
                        generatedDir.createDirectories()

                        val (pe, rootElements, sources) = psiClassesToApt(psiCaches)
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
                                val path = generatedDir.resolve(it.packageName.replace(".", "/"))
                                    .resolve("${it.fileName}.${it.extensionName}")
                                path.parent.createDirectories()
                                path.writeText(it.content)
                                VirtualFileManager.getInstance().asyncRefresh()
                            }
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                        javaImmutablePsiClassCache.addAll(psiCaches)
                    }
                }
            }
        }
    }

    fun dtoProcessJava(project: Project, dtoFiles: List<Path>) {
        ApplicationManager.getApplication().executeOnPooledThread {
            ApplicationManager.getApplication().runReadAction {
                if (!DumbService.isDumb(project)) {
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
                        }, findProjectDir(it)?.absolutePathString() ?: "", "", emptyList(), it.name)
                        val compiler = AptDtoCompiler(dtoFile, elements, DtoModifier.STATIC)
                        val typeElement: TypeElement =
                            elements.getTypeElement(compiler.sourceTypeName) ?: return@forEach
                        val compile = compiler.compile(context.getImmutableType(typeElement))
                        compile.forEach {
                            DtoGenerator(context, DocMetadata(context), it).generate()
                        }
                        val projectDir = findProjectDir(it) ?: return@forEach
                        val generatedDir = getGeneratedDir(project, projectDir) ?: return@forEach
                        sources.forEach {
                            val path = generatedDir.resolve(it.packageName.replace(".", "/"))
                                .resolve("${it.fileName}.${it.extensionName}")
                            path.parent.createDirectories()
                            path.writeText(it.content)
                            VirtualFileManager.getInstance().asyncRefresh()
                        }
                    }
                }
            }
        }
    }

    fun sourceProcessKotlin(project: Project, projects: Map<Path, List<Path>>) {
        ApplicationManager.getApplication().executeOnPooledThread {
            ApplicationManager.getApplication().runReadAction {
                if (!DumbService.isDumb(project)) {
                    projects.forEach { projectDir, sourceFiles ->
                        val ktClassCaches =
                            if (projects.size == 1) kotlinImmutableKtClassCache else CopyOnWriteArrayList()
                        sourceFiles.forEach {
                            val psiFile =
                                VirtualFileManager.getInstance().findFileByNioPath(it)!!.toPsiFile(project)!!
                            ktClassCaches.addAll(psiFile.children.mapNotNull { psi ->
                                return@mapNotNull if (psi is KtClass) {
                                    if (PSI_SHARED.annotations(psi).any { annotation ->
                                            val fqName = annotation.fqName
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

                        val generatedDir = getGeneratedDir(project, projectDir) ?: return@forEach
                        generatedDir.createDirectories()

                        val (resolver, environment, sources) = ktClassToKsp(ktClassCaches)
                        try {
                            val context = Context(resolver, environment)
                            org.babyfish.jimmer.ksp.immutable.ImmutableProcessor(context, false).process()
                            org.babyfish.jimmer.ksp.error.ErrorProcessor(context, true).process()
                            sources.forEach { source ->
                                val path = generatedDir.resolve(source.packageName.replace(".", "/"))
                                    .resolve("${source.fileName}.${source.extensionName}")
                                path.parent.createDirectories()
                                path.writeText(source.content)
                                VirtualFileManager.getInstance().asyncRefresh()
                            }
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                        kotlinImmutableKtClassCache.addAll(ktClassCaches)
                    }
                }
            }
        }
    }

    fun dtoProcessKotlin(project: Project, dtoFiles: List<Path>) {
        ApplicationManager.getApplication().executeOnPooledThread {
            ApplicationManager.getApplication().runReadAction {
                DumbService.getInstance(project).runWhenSmart {
                    val (resolver, environment, sources) = ktClassToKsp(kotlinImmutableKtClassCache)
                    val context = Context(resolver, environment)
                    dtoFiles.forEach {
                        val dtoFile = DtoFile(object : OsFile {
                            override fun getAbsolutePath(): String {
                                return it.absolutePathString()
                            }

                            override fun openReader(): Reader {
                                return it.readText().reader()
                            }
                        }, findProjectDir(it)?.absolutePathString() ?: "", "", emptyList(), it.name)
                        val compiler = KspDtoCompiler(dtoFile, context.resolver, DtoModifier.STATIC)
                        val classDeclarationByName =
                            resolver.getClassDeclarationByName(compiler.sourceTypeName) ?: return@forEach
                        val compile = compiler.compile(context.typeOf(classDeclarationByName))
                        compile.forEach {
                            org.babyfish.jimmer.ksp.dto.DtoGenerator(
                                context,
                                org.babyfish.jimmer.ksp.client.DocMetadata(context),
                                false,
                                it,
                                context.environment.codeGenerator
                            ).generate(emptyList())
                        }
                        val projectDir = findProjectDir(it) ?: return@forEach
                        val generatedDir = getGeneratedDir(project, projectDir) ?: return@forEach
                        generatedDir.createDirectories()
                        sources.forEach { source ->
                            val path = generatedDir.resolve(source.packageName.replace(".", "/"))
                                .resolve("${source.fileName}.${source.extensionName}")
                            path.parent.createDirectories()
                            path.writeText(source.content)
                            VirtualFileManager.getInstance().asyncRefresh()
                        }
                    }
                }
            }
        }
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

    private fun getGeneratedDir(project: Project, projectDir: Path): Path? {
        if (isJavaProject(project)) {
            return if (isMavenProject(projectDir)) {
                projectDir.resolve("target/generated/sources/annotationProcessor/java/main")
            } else if (isGradleProject(projectDir)) {
                projectDir.resolve("build/generated/sources/annotationProcessor/java/main")
            } else {
                null
            }
        } else if (isKotlinProject(project)) {
            return if (isGradleProject(projectDir)) {
                projectDir.resolve("build/generated/ksp/main/kotlin")
            } else if (isMavenProject(projectDir)) {
                projectDir.resolve("target/generated/ksp/main/kotlin")
            } else {
                null
            }
        } else {
            return null
        }
    }
}