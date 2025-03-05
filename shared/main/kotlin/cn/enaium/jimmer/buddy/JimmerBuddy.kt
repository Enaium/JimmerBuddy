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
import cn.enaium.jimmer.buddy.wizard.JimmerProjectModel
import com.google.devtools.ksp.getClassDeclarationByName
import com.intellij.compiler.CompilerConfiguration
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFileFactory
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
import org.babyfish.jimmer.ksp.Context
import org.babyfish.jimmer.ksp.KspDtoCompiler
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.core.util.toVirtualFile
import org.jetbrains.kotlin.psi.KtClass
import java.io.File
import java.io.Reader
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import kotlin.io.path.*


/**
 * @author Enaium
 */
object JimmerBuddy {

    const val NAME = "JimmerBuddy"
    const val MODULE_TYPE_ID = "JimmerModuleType"
    const val JIMMER_NAME = "Jimmer"
    val PROJECT_MODEL_PROP_KEY = Key<GraphProperty<JimmerProjectModel>>("vaadin_project_model")

    object Icons {
        val LOGO = IconLoader.getIcon("/icons/logo.svg", JimmerBuddy::class.java)
        val LOGO_NORMAL = IconLoader.getIcon("/icons/normal.svg", JimmerBuddy::class.java)
        val IMMUTABLE = IconLoader.getIcon("/icons/immutable.svg", JimmerBuddy::class.java)
        val DTO = IconLoader.getIcon("/icons/dto.svg", JimmerBuddy::class.java)
    }

    private val javaImmutablePsiClassCache = CopyOnWriteArraySet<PsiClass>()

    private val kotlinImmutableKtClassCache = CopyOnWriteArraySet<KtClass>()

    var isInit = false

    val LOG: Log by lazy { Log(ProjectManager.getInstance().defaultProject) }

    val DEQ = DelayedExecutionQueue(2000)

    val PSI_SHARED: PsiShared = ServiceLoader.load(PsiShared::class.java, JimmerBuddy::class.java.classLoader).first()

    fun init() {
        if (isInit) return
        if (DumbService.isDumb(ProjectManager.getInstance().openProjects.firstOrNull() ?: return)) {
            return
        }
        LOG.info("JimmerBuddy is initializing")
        isInit = true
        val project = ProjectManager.getInstance().openProjects.takeIf { it.isNotEmpty() }?.first() ?: return
        val projects =
            findProjects(project.guessProjectDir()?.toNioPath()!!)
        ApplicationManager.getApplication().executeOnPooledThread {
            ApplicationManager.getApplication().runReadAction {
                if (!DumbService.isDumb(project)) {
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
                            projects.map { it.resolve("src/main/dto").walk().filter { it.extension == "dto" }.toList() }
                                .flatten()
                        )
                    }
                }
            }
        }
        LOG.info("JimmerBuddy is initialized")
    }

    fun initialize() {
        reset()
        init()
    }

    fun reset() {
        javaImmutablePsiClassCache.clear()
        kotlinImmutableKtClassCache.clear()
        isInit = false
    }

    fun sourcesProcessJava(project: Project, projects: Map<Path, List<Path>>) {
        LOG.info("SourcesProcessJava Project:${projects.map { it.key.name }}")
        projects.forEach { (projectDir, sourceFiles) ->
            sourceFiles.isEmpty() && return@forEach
            val psiCaches = CopyOnWriteArraySet<PsiClass>()
            sourceFiles.forEach {
                val psiFile =
                    it.toFile().toVirtualFile()?.findPsiFile(project) ?: PsiFileFactory.getInstance(project)
                        .createFileFromText("dummy.java", JavaFileType.INSTANCE, it.readText())
                psiCaches.addAll(psiFile.children.mapNotNull { psi ->
                    return@mapNotNull if (psi is PsiClass) {
                        if (psi.isJimmerImmutableType().not()) return@mapNotNull null
                        psi
                    } else {
                        null
                    }
                })
            }

            LOG.info("SourcesProcessJava Project:${projectDir.name} PsiCaches:${javaImmutablePsiClassCache.size}")

            val generatedDir = getGeneratedDir(project, projectDir) ?: return@forEach
            generatedDir.createDirectories()

            val (pe, rootElements, sources) = psiClassesToApt(psiCaches, javaImmutablePsiClassCache)
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
                asyncRefresh(sources.map {
                    val path = generatedDir.resolve(it.packageName.replace(".", "/"))
                        .resolve("${it.fileName}.${it.extensionName}")
                    path.parent.createDirectories()
                    path.writeText(it.content)
                    path
                })
            } catch (e: Throwable) {
                LOG.error(e)
            }
            javaImmutablePsiClassCache.addAll(psiCaches)
        }
    }

    fun dtoProcessJava(project: Project, dtoFiles: List<Path>) {
        LOG.info("DtoProcessJava Project:${dtoFiles.joinToString(", ") { it.name }}")
        val (pe, rootElements, sources) = psiClassesToApt(CopyOnWriteArraySet(), javaImmutablePsiClassCache)
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
            try {
                val compile = compiler.compile(context.getImmutableType(typeElement))
                compile.forEach {
                    DtoGenerator(context, DocMetadata(context), it).generate()
                }
                val projectDir = findProjectDir(it) ?: return@forEach
                val generatedDir = getGeneratedDir(project, projectDir) ?: return@forEach
                asyncRefresh(sources.map {
                    val path = generatedDir.resolve(it.packageName.replace(".", "/"))
                        .resolve("${it.fileName}.${it.extensionName}")
                    path.parent.createDirectories()
                    path.writeText(it.content)
                    path
                })
            } catch (e: Throwable) {
                LOG.error(e)
            }
        }
    }

    fun sourceProcessKotlin(project: Project, projects: Map<Path, List<Path>>) {
        LOG.info("SourceProcessKotlin Project:${projects.map { it.key.name }}")
        projects.forEach { projectDir, sourceFiles ->
            sourceFiles.isEmpty() && return@forEach
            val ktClassCaches = CopyOnWriteArraySet<KtClass>()
            sourceFiles.forEach {
                val psiFile = it.toFile().toPsiFile(project)!!
                ktClassCaches.addAll(psiFile.children.mapNotNull { psi ->
                    return@mapNotNull if (psi is KtClass) {
                        if (psi.isJimmerImmutableType().not()) return@mapNotNull null
                        psi
                    } else {
                        null
                    }
                })
            }

            LOG.info("SourceProcessKotlin Project:${projectDir.name} KtClassCaches:${kotlinImmutableKtClassCache.size}")

            val generatedDir = getGeneratedDir(project, projectDir) ?: return@forEach
            generatedDir.createDirectories()

            val (resolver, environment, sources) = ktClassToKsp(ktClassCaches, kotlinImmutableKtClassCache)
            try {
                val context = Context(resolver, environment)
                org.babyfish.jimmer.ksp.immutable.ImmutableProcessor(context, false).process()
                org.babyfish.jimmer.ksp.error.ErrorProcessor(context, true).process()
                asyncRefresh(files = sources.map { source ->
                    val path = generatedDir.resolve(source.packageName.replace(".", "/"))
                        .resolve("${source.fileName}.${source.extensionName}")
                    path.parent.createDirectories()
                    path.writeText(source.content)
                    path
                })
            } catch (e: Throwable) {
                LOG.error(e)
            }
            kotlinImmutableKtClassCache.addAll(ktClassCaches)
        }
    }

    fun dtoProcessKotlin(project: Project, dtoFiles: List<Path>) {
        LOG.info("DtoProcessKotlin Project:${dtoFiles.joinToString(", ") { it.name }}")
        val (resolver, environment, sources) = ktClassToKsp(
            CopyOnWriteArraySet(),
            kotlinImmutableKtClassCache
        )
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
            try {
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
                asyncRefresh(sources.map { source ->
                    val path = generatedDir.resolve(source.packageName.replace(".", "/"))
                        .resolve("${source.fileName}.${source.extensionName}")
                    path.parent.createDirectories()
                    path.writeText(source.content)
                    path
                })
            } catch (e: Throwable) {
                LOG.error(e)
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

        return project.modules.any { module ->
            CompilerConfiguration.getInstance(project).getAnnotationProcessingConfiguration(module).processorPath.split(
                File.pathSeparator
            ).any { it.contains("jimmer-apt") }
        }
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
                projectDir.resolve("target/generated-sources/annotations")
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

    fun asyncRefresh(files: List<Path>) {
        files.isEmpty() && return
        ApplicationManager.getApplication().invokeLater {
            try {
                LocalFileSystem.getInstance().refreshNioFiles(files)
                LOG.info("Refreshed ${files.joinToString(", ") { it.name }}")
            } catch (_: Throwable) {
            }
        }
    }
}