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

import cn.enaium.jimmer.buddy.extensions.index.AnnotationClassIndex
import cn.enaium.jimmer.buddy.extensions.index.EnumClassIndex
import cn.enaium.jimmer.buddy.extensions.index.FullClassIndex
import cn.enaium.jimmer.buddy.extensions.index.InterfaceClassIndex
import cn.enaium.jimmer.buddy.extensions.wizard.JimmerProjectModel
import cn.enaium.jimmer.buddy.service.NavigationService
import cn.enaium.jimmer.buddy.service.PsiService
import cn.enaium.jimmer.buddy.service.UiService
import cn.enaium.jimmer.buddy.utility.*
import com.google.devtools.ksp.getClassDeclarationByName
import com.intellij.compiler.CompilerConfiguration
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.progress.withBackgroundProgress
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiClass
import com.intellij.util.indexing.ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.babyfish.jimmer.apt.client.DocMetadata
import org.babyfish.jimmer.apt.createAptOption
import org.babyfish.jimmer.apt.dto.AptDtoCompiler
import org.babyfish.jimmer.apt.dto.DtoGenerator
import org.babyfish.jimmer.apt.entry.EntryProcessor
import org.babyfish.jimmer.apt.error.ErrorProcessor
import org.babyfish.jimmer.apt.immutable.ImmutableProcessor
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType
import org.babyfish.jimmer.ksp.Context
import org.babyfish.jimmer.ksp.KspDtoCompiler
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.core.util.toVirtualFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap
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
    const val JIMMER_NAME = "Jimmer"
    const val DTO_NAME = "Jimmer DTO"
    const val DTO_LANGUAGE_ID = "JimmerBuddy.DTO"
    val PROJECT_MODEL_PROP_KEY = Key<GraphProperty<JimmerProjectModel>>("jimmer_project_model")

    object Indexes {
        val ANNOTATION_CLASS = ID.create<String, Void>(AnnotationClassIndex::class.qualifiedName!!)
        val INTERFACE_CLASS = ID.create<String, Void>(InterfaceClassIndex::class.qualifiedName!!)
        val ENUM_CLASS = ID.create<String, Void>(EnumClassIndex::class.qualifiedName!!)
        val FULL_CLASS = ID.create<String, Void>(FullClassIndex::class.qualifiedName!!)
    }

    object Icons {
        object Nodes
        object Databases
    }

    const val INFO_GROUP_ID = "JimmerBuddy.NotificationGroup"

    val DEQ = DelayedExecutionQueue()

    object Services {
        val PSI: PsiService = ServiceLoader.load(PsiService::class.java, JimmerBuddy::class.java.classLoader).first()
        val UI: UiService = ServiceLoader.load(UiService::class.java, JimmerBuddy::class.java.classLoader).first()
        val NAVIGATION: NavigationService =
            ServiceLoader.load(NavigationService::class.java, JimmerBuddy::class.java.classLoader).first()
    }

    private val workspaces = ConcurrentHashMap<Project, Workspace>()

    fun getWorkspace(project: Project): Workspace {
        return workspaces[project] ?: Workspace(project).also {
            workspaces[project] = it
        }
    }

    data class GenerateProject(
        val projectDir: Path,
        val sourceFiles: Set<Path>,
        val src: String
    ) {
        companion object {
            suspend fun generate(
                projects: Set<Path>,
                srcSets: Set<String>,
                sourceRootTypes: List<SourceRootType>
            ): Set<GenerateProject> =
                sourceRootTypes.map { generate(projects, srcSets, it) }.flatten().toSet()

            suspend fun generate(
                projects: Set<Path>,
                srcSets: Set<String>,
                sourceRootType: SourceRootType
            ): Set<GenerateProject> =
                withContext(Dispatchers.IO) {
                    projects.map { project ->
                        srcSets.map { src ->
                            GenerateProject(
                                projectDir = project,
                                project.resolve("src/${src}/${sourceRootType.dir}").walk()
                                    .filter { it.extension == sourceRootType.extension }.toSet(),
                                src
                            )
                        }
                    }.flatten().toSet()
                }

            suspend fun generate(sourceFile: Path, sourceRootTypes: List<SourceRootType>): Set<GenerateProject> =
                sourceRootTypes.map { generate(sourceFile, it) }.flatten().toSet()

            suspend fun generate(sourceFile: Path, sourceRootType: SourceRootType): Set<GenerateProject> =
                withContext(Dispatchers.IO) {
                    setOfNotNull(findProjectDir(sourceFile)?.let { project ->
                        project.resolve("src").toFile().walk().maxDepth(1)
                            .find { sourceFile.startsWith(it.toPath().resolve(sourceRootType.dir)) }?.name?.let { src ->
                                GenerateProject(project, setOf(sourceFile), src)
                            }
                    })
                }
        }

        enum class SourceRootType(val dir: String, val extension: String) {
            JAVA("java", "java"),
            KOTLIN("kotlin", "kt"),
            JAVA_KOTLIN("java", "kt"),
            JVM_MAIN_KOTLIN("jvmMain", "kt"),
            ANDROID_MAIN_KOTLIN("androidMain", "kt"),
            DTO("dto", "dto")
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as GenerateProject

            if (projectDir != other.projectDir) return false
            if (sourceFiles != other.sourceFiles) return false
            if (src != other.src) return false

            return true
        }

        override fun hashCode(): Int {
            var result = projectDir.hashCode()
            result = 31 * result + sourceFiles.hashCode()
            result = 31 * result + src.hashCode()
            return result
        }
    }

    class Workspace(val project: Project) {

        val log = Log(project)

        val javaImmutablePsiClassCache = CopyOnWriteArraySet<PsiClass>()

        var init = false
        var initialized = false

        fun init(enable: Boolean = true) {
            if (init) return
            if (project.isDumb()) return
            init = true
            CoroutineScope(Dispatchers.Default).launch {
                val projects = project.findProjects()
                log.info("Project ${project.name} is initializing")
                if (project.isJavaProject()) {
                    val generateProject = GenerateProject.generate(
                        projects,
                        setOf("main", "test"),
                        GenerateProject.SourceRootType.JAVA
                    )
                    if (enable) {
                        sourcesProcessJava(generateProject)
                        dtoProcessJava(
                            GenerateProject.generate(
                                projects,
                                setOf("main", "test"),
                                GenerateProject.SourceRootType.DTO
                            )
                        )
                    }
                } else if (project.isKotlinProject()) {
                    if (enable) {
                        sourcesProcessKotlin(
                            GenerateProject.generate(
                                projects,
                                setOf("main", "test"),
                                listOf(GenerateProject.SourceRootType.KOTLIN) +
                                        if (project.isAndroidProject()) {
                                            listOf(
                                                GenerateProject.SourceRootType.JAVA_KOTLIN,
                                                GenerateProject.SourceRootType.JVM_MAIN_KOTLIN,
                                                GenerateProject.SourceRootType.ANDROID_MAIN_KOTLIN
                                            )
                                        } else {
                                            emptyList()
                                        }
                            )
                        )
                        dtoProcessKotlin(
                            GenerateProject.generate(
                                projects,
                                setOf("main", "test"),
                                GenerateProject.SourceRootType.DTO
                            )
                        )
                    }
                }
                log.info("Project ${project.name} is initialized")
                initialized = true
            }
        }

        fun initialize() {
            reset()
            init()
        }

        fun reset() {
            javaImmutablePsiClassCache.clear()
            init = false
        }

        private fun getGeneratedDir(project: Project, projectDir: Path, src: String): Path? {
            if (project.isJavaProject()) {
                return if (isMavenProject(projectDir)) {
                    projectDir.resolve("target/generated-sources/annotations")
                } else if (isGradleProject(projectDir)) {
                    if (project.isAndroidProject()) {
                        projectDir.resolve("build/generated/ap_generated_sources/debug/out")
                    } else {
                        projectDir.resolve("build/generated/sources/annotationProcessor/java/${src}")
                    }
                } else {
                    null
                }
            } else if (project.isKotlinProject()) {
                return if (isGradleProject(projectDir)) {
                    if (project.isAndroidProject()) {
                        projectDir.resolve("build/generated/ksp/debug/kotlin")
                    } else {
                        projectDir.resolve("build/generated/ksp/${src}/kotlin")
                    }
                } else if (isMavenProject(projectDir)) {
                    projectDir.resolve("target/generated/ksp/${src}/kotlin")
                } else {
                    null
                }
            } else {
                return null
            }
        }

        suspend fun asyncRefreshSources(sources: List<Pair<Source, Path>>) {
            asyncRefresh(files = sources.map { source ->
                withContext(Dispatchers.IO) {
                    val path = source.second.resolve(source.first.packageName.replace(".", "/"))
                        .resolve("${source.first.fileName}.${source.first.extensionName}")
                    path.createParentDirectories()
                    path.writeText(source.first.content)
                    path
                }
            })
        }

        fun asyncRefresh(files: List<Path>) {
            files.isEmpty() && return
            project.runWhenSmart {
                CoroutineScope(Dispatchers.IO).launch {
                    LocalFileSystem.getInstance().refreshNioFiles(files)
                }
                invokeLater {
                    log.info("Refreshed ${files.joinToString(", ") { it.name }}")
                }
            }
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

        suspend fun sourcesProcessJava(projects: Set<GenerateProject>) {
            withBackgroundProgress(project, "Processing Java Source") {
                val needRefresh = mutableListOf<Pair<Source, Path>>()

                projects.forEach {
                    it.sourceFiles.forEach { sourceFile ->
                        ReadAction.run<Throwable> {
                            val psiFile =
                                sourceFile.toFile().toVirtualFile()?.findPsiFile(project) ?: return@run
                            psiFile.getChildOfType<PsiClass>()?.takeIf { it.hasJimmerAnnotation() }?.also {
                                javaImmutablePsiClassCache.add(it)
                            }
                        }
                    }
                }

                projects.forEach { (projectDir, sourceFiles, src) ->
                    sourceFiles.isEmpty() && return@forEach
                    val psiCaches = CopyOnWriteArraySet<PsiClass>()
                    ReadAction.run<Throwable> {
                        sourceFiles.forEach { sourceFile ->
                            val psiFile = sourceFile.toFile().toVirtualFile()?.findPsiFile(project) ?: return@forEach
                            psiFile.getChildOfType<PsiClass>()?.takeIf { it.hasJimmerAnnotation() }?.also {
                                psiCaches.add(it)
                            }
                        }

                        val generatedDir = getGeneratedDir(project, projectDir, src) ?: return@run

                        val (pe, rootElements, sources) = project.psiClassesToApt(javaImmutablePsiClassCache)
                        val currentModule = project.modules.find { it.name.endsWith(".$src") }
                        val aptOptions = currentModule?.let { module ->
                            CompilerConfiguration.getInstance(project)
                                .getAnnotationProcessingConfiguration(module).processorOptions
                        } ?: emptyMap()

                        log.info("SourcesProcessJava Project:${projectDir.name}:${src} APTOptions:${aptOptions}")

                        val option = createAptOption(
                            aptOptions,
                            pe.elementUtils,
                            pe.typeUtils,
                            pe.filer
                        )
                        val roundEnv = createRoundEnvironment(rootElements)

                        try {
                            val immutableProcessor = ImmutableProcessor(option.context, pe.messager)
                            val immutableTypeElements = try {
                                ImmutableProcessor::class.java.getDeclaredMethod(
                                    "parseImmutableTypes",
                                    RoundEnvironment::class.java
                                )
                            } catch (_: Throwable) {
                                null
                            }?.let {
                                it.isAccessible = true
                                @Suppress("UNCHECKED_CAST")
                                it.invoke(immutableProcessor, roundEnv) as Map<TypeElement, ImmutableType>
                            } ?: emptyMap()

                            ImmutableProcessor::class.java.declaredMethods.find { it.name == "generateJimmerTypes" }
                                ?.also {
                                    it.isAccessible = true
                                    it.invoke(
                                        immutableProcessor,
                                        immutableTypeElements.filter { ite ->
                                            psiCaches.mapNotNull { it.qualifiedName }
                                                .any { it == ite.value.qualifiedName }
                                        })
                                }

                            EntryProcessor(option.context, immutableTypeElements.keys).process()
                            ErrorProcessor(option.context, option.checkedException).process(roundEnv)
                            sources.forEach {
                                needRefresh.add(it to generatedDir)
                            }
                        } catch (e: Throwable) {
                            log.error(e)
                        }
                    }
                }
                asyncRefreshSources(needRefresh)
            }
        }

        suspend fun dtoProcessJava(projects: Set<GenerateProject>) {
            withBackgroundProgress(project, "Processing Java DTO") {
                val needRefresh = mutableListOf<Pair<Source, Path>>()
                val (pe, rootElements, sources) = project.psiClassesToApt(emptySet())
                ReadAction.run<Throwable> {
                    projects.forEach { (projectDir, sourceFiles, src) ->
                        sourceFiles.forEach { sourceFile ->
                            val currentModule = project.modules.find { it.name.endsWith(".$src") }
                            val aptOptions = currentModule?.let { module ->
                                CompilerConfiguration.getInstance(project)
                                    .getAnnotationProcessingConfiguration(module).processorOptions
                            } ?: emptyMap()
                            log.info("DtoProcessJava Project:${projectDir.name}:${src} APTOptions:${aptOptions}")
                            val option = createAptOption(
                                aptOptions,
                                pe.elementUtils,
                                pe.typeUtils,
                                pe.filer
                            )

                            sourceFiles.isEmpty() && return@forEach
                            val elements = pe.elementUtils
                            val dtoFile = project.toDtoFile(projectDir, sourceFile)
                            try {
                                val compiler = AptDtoCompiler(dtoFile, elements, option.defaultNullableInputModifier)
                                val typeElement: TypeElement =
                                    elements.getTypeElement(compiler.sourceTypeName) ?: return@forEach
                                val compile = compiler.compile(option.context.getImmutableType(typeElement))
                                compile.forEach {
                                    DtoGenerator(option.context, DocMetadata(option.context), it).generate()
                                }
                                val generatedDir = getGeneratedDir(project, projectDir, src) ?: return@forEach
                                sources.forEach {
                                    needRefresh.add(it to generatedDir)
                                }
                            } catch (e: Throwable) {
                                log.error(e)
                            }
                        }
                    }
                }
                asyncRefreshSources(needRefresh)
            }
        }

        suspend fun sourcesProcessKotlin(projects: Set<GenerateProject>) {
            withBackgroundProgress(project, "Processing Kotlin Source") {
                val needRefresh = mutableListOf<Pair<Source, Path>>()
                projects.forEach { (projectDir, sourceFiles, src) ->
                    sourceFiles.isEmpty() && return@forEach
                    project.runReadActionSmart {
                        val generatedDir = getGeneratedDir(project, projectDir, src) ?: return@runReadActionSmart
                        val (resolver, environment, sources) = project.ktClassesToKsp(
                            sourceFiles.mapNotNull { sourceFile ->
                                sourceFile.toFile().toPsiFile(project)?.getChildOfType<KtClass>()
                                    ?.takeIf { it.hasJimmerAnnotation() }
                            }.toSet()
                        )
                        try {
                            val kspOptions = getKspOptions(project)

                            log.info("SourcesProcessKotlin Project:${projectDir.name}:${src} KSPOptions:${kspOptions}")

                            val option = createKspOption(
                                kspOptions,
                                Context(resolver, environment),
                                environment.codeGenerator
                            )
                            org.babyfish.jimmer.ksp.immutable.ImmutableProcessor(
                                option.context,
                                option.isModuleRequired,
                                option.excludedUserAnnotationPrefixes
                            )
                                .process()
                            org.babyfish.jimmer.ksp.error.ErrorProcessor(option.context, option.checkedException)
                                .process()
                            sources.forEach {
                                needRefresh.add(it to generatedDir)
                            }
                        } catch (e: Throwable) {
                            log.error(e)
                        }
                    }
                }
                asyncRefreshSources(needRefresh)
            }
        }

        suspend fun dtoProcessKotlin(projects: Set<GenerateProject>) {
            withBackgroundProgress(project, "Processing Kotlin DTO") {
                val needRefresh = mutableListOf<Pair<Source, Path>>()
                project.runReadActionSmart {
                    val (resolver, environment, sources) = project.ktClassesToKsp(emptySet())
                    projects.forEach { (projectDir, sourceFiles, src) ->
                        sourceFiles.isEmpty() && return@forEach

                        val kspOptions = getKspOptions(project)

                        log.info("DtoProcessKotlin Project:${projectDir.name}:${src} KSPOptions:${kspOptions}")

                        val option = createKspOption(
                            kspOptions,
                            Context(resolver, environment),
                            environment.codeGenerator
                        )

                        sourceFiles.forEach { sourceFile ->
                            val dtoFile = project.toDtoFile(projectDir, sourceFile)
                            try {
                                val compiler =
                                    KspDtoCompiler(
                                        dtoFile,
                                        option.context.resolver,
                                        option.defaultNullableInputModifier
                                    )
                                val classDeclarationByName =
                                    resolver.getClassDeclarationByName(compiler.sourceTypeName) ?: return@forEach
                                val compile = compiler.compile(option.context.typeOf(classDeclarationByName))
                                compile.forEach { dtoType ->
                                    org.babyfish.jimmer.ksp.dto.DtoGenerator(
                                        option.context,
                                        org.babyfish.jimmer.ksp.client.DocMetadata(option.context),
                                        option.mutable,
                                        dtoType,
                                        option.context.environment.codeGenerator
                                    ).generate(emptyList())
                                }
                                val generatedDir = getGeneratedDir(project, projectDir, src) ?: return@forEach
                                sources.forEach { source ->
                                    needRefresh.add(source to generatedDir)
                                }
                            } catch (e: Throwable) {
                                log.error(e)
                            }
                        }
                    }
                }
                asyncRefreshSources(needRefresh)
            }
        }
    }
}