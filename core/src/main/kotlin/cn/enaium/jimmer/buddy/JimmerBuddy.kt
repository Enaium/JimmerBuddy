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
import cn.enaium.jimmer.buddy.storage.JimmerBuddySetting
import cn.enaium.jimmer.buddy.utility.*
import com.google.devtools.ksp.getClassDeclarationByName
import com.intellij.compiler.CompilerConfiguration
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.progress.withBackgroundProgress
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiClass
import com.intellij.util.indexing.ID
import kotlinx.coroutines.*
import org.babyfish.jimmer.apt.client.DocMetadata
import org.babyfish.jimmer.apt.createAptOption
import org.babyfish.jimmer.apt.dto.AptDtoCompiler
import org.babyfish.jimmer.apt.dto.DtoGenerator
import org.babyfish.jimmer.apt.entry.EntryProcessor
import org.babyfish.jimmer.apt.error.ErrorProcessor
import org.babyfish.jimmer.apt.immutable.ImmutableProcessor
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType
import org.babyfish.jimmer.apt.tuple.TypedTupleProcessor
import org.babyfish.jimmer.ksp.Context
import org.babyfish.jimmer.ksp.KspDtoCompiler
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.core.util.toVirtualFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
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
                sourceRootTypes: List<SourceRootType>
            ): Set<GenerateProject> =
                sourceRootTypes.map { generate(projects, it) }.flatten().toSet()

            suspend fun generate(
                projects: Set<Path>,
                sourceRootType: SourceRootType
            ): Set<GenerateProject> =
                withContext(Dispatchers.IO) {
                    projects.map { project ->
                        JimmerBuddySetting.INSTANCE.state.srcSets.split(",").map { src ->
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
        var isJavaProject = false
        var isKotlinProject = false
        var isAndroidProject = false

        fun init(enable: Boolean = true) {
            if (init) return
            if (project.isDumb()) return
            init = true
            CoroutineScope(Dispatchers.Default).launch {
                val projects = project.findProjects()
                log.info("Project ${project.name} is initializing")
                isJavaProject = project.isJavaProject()
                isKotlinProject = project.isKotlinProject()
                isAndroidProject = project.isAndroidProject()

                if (isJavaProject) {
                    val generateProject = GenerateProject.generate(
                        projects,
                        GenerateProject.SourceRootType.JAVA
                    )
                    if (enable) {
                        sourcesProcessJava(generateProject)
                        dtoProcessJava(
                            GenerateProject.generate(
                                projects,
                                GenerateProject.SourceRootType.DTO
                            )
                        )
                    }
                } else if (isKotlinProject) {
                    if (enable) {
                        sourcesProcessKotlin(
                            GenerateProject.generate(
                                projects,
                                listOf(
                                    GenerateProject.SourceRootType.KOTLIN,
                                    GenerateProject.SourceRootType.JAVA_KOTLIN
                                )
                            )
                        )
                        dtoProcessKotlin(
                            GenerateProject.generate(
                                projects,
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
            if (isJavaProject) {
                return if (isMavenProject(projectDir)) {
                    projectDir.resolve("target/generated-sources/annotations")
                } else if (isGradleProject(projectDir)) {
                    if (isAndroidProject) {
                        projectDir.resolve("build/generated/ap_generated_sources/debug/out")
                    } else {
                        projectDir.resolve("build/generated/sources/annotationProcessor/java/${src}")
                    }
                } else {
                    null
                }
            } else if (isKotlinProject) {
                return if (isGradleProject(projectDir)) {
                    if (isAndroidProject) {
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

        fun asyncRefreshSources(sources: Iterable<Pair<Source, Path>>) {
            asyncRefresh(files = sources.map { source ->
                val path = source.second.resolve(source.first.packageName.replace(".", "/"))
                    .resolve("${source.first.fileName}.${source.first.extensionName}")
                path.createParentDirectories()
                path.writeText(source.first.content)
                path
            })
        }

        fun asyncRefresh(files: List<Path>) {
            files.isEmpty() && return
            project.runWhenSmart {
                VfsUtil.markDirtyAndRefresh(true, true, true, *files.map { it.toFile() }.toTypedArray())
                log.info("Refreshed ${files.joinToString(", ") { it.name }}")
            }
        }

        private fun createRoundEnvironment(
            rootElements: Set<Element>,
        ): RoundEnvironment {
            return object : RoundEnvironment {
                override fun processingOver(): Boolean {
                    TODO("Not yet implemented")
                }

                override fun errorRaised(): Boolean {
                    TODO("Not yet implemented")
                }

                override fun getRootElements(): Set<Element> {
                    return rootElements
                }

                override fun getElementsAnnotatedWith(a: TypeElement): Set<Element> {
                    TODO("Not yet implemented")
                }

                override fun getElementsAnnotatedWith(a: Class<out Annotation>): Set<Element> {
                    return rootElements.filter { it.getAnnotation(a as Class<Annotation>) != null }
                        .toSet()
                }
            }
        }

        suspend fun sourcesProcessJava(projects: Set<GenerateProject>) {
            withBackgroundProgress(project, "Processing Java Source") {
                val needRefresh = ConcurrentLinkedQueue<Pair<Source, Path>>()

                supervisorScope {
                    projects.forEach { (projectDir, sourceFiles, src) ->
                        sourceFiles.isEmpty() && return@forEach

                        val job = launch {
                            val refreshItems = project.readActionSmartCoroutine {
                                val psiCaches = mutableSetOf<PsiClass>()
                                sourceFiles.forEach { sourceFile ->
                                    val psiFile =
                                        sourceFile.toFile().toVirtualFile()?.findPsiFile(project)
                                            ?: return@forEach
                                    psiFile.getChildOfType<PsiClass>()?.takeIf { it.hasJimmerAnnotation() }
                                        ?.also { psiCaches.add(it) }
                                }

                                if (psiCaches.isEmpty()) {
                                    return@readActionSmartCoroutine emptyList()
                                }

                                val generatedDir =
                                    getGeneratedDir(project, projectDir, src)
                                        ?: return@readActionSmartCoroutine emptyList()

                                val (pe, rootElements, sources) = project.psiClassesToApt(psiCaches)
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
                                        it.invokeWithCatch(
                                            immutableProcessor,
                                            roundEnv
                                        ) as Map<TypeElement, ImmutableType>
                                    } ?: emptyMap()

                                    val qualifiedNames: Set<String> = psiCaches
                                        .mapNotNullTo(mutableSetOf()) { it.qualifiedName }

                                    ImmutableProcessor::class.java.declaredMethods.find { it.name == "generateJimmerTypes" }
                                        ?.also {
                                            it.isAccessible = true
                                            it.invokeWithCatch(
                                                immutableProcessor,
                                                immutableTypeElements.filter { ite ->
                                                    ite.value.qualifiedName in qualifiedNames
                                                })
                                        }

                                    EntryProcessor(option.context, immutableTypeElements.keys).process()
                                    ErrorProcessor(option.context, option.checkedException).process(roundEnv)
                                    TypedTupleProcessor(
                                        option.context,
                                        psiCaches.filter { it.hasTypedTupleAnnotation() }
                                            .mapNotNull { it.qualifiedName }.toSet()
                                    ).process(roundEnv)
                                    sources.map { it to generatedDir }
                                } catch (e: Throwable) {
                                    if (e is ControlFlowException) {
                                        throw e
                                    }
                                    log.error(e)
                                    emptyList()
                                }
                            }
                            needRefresh.addAll(refreshItems)
                        }
                        job.invokeOnCompletion { ex ->
                            if (ex != null && ex !is CancellationException && ex !is ControlFlowException) {
                                log.error(ex)
                            }
                        }
                    }
                }

                asyncRefreshSources(needRefresh)
            }
        }

        suspend fun dtoProcessJava(projects: Set<GenerateProject>) {
            withBackgroundProgress(project, "Processing Java DTO") {
                val needRefresh = ConcurrentLinkedQueue<Pair<Source, Path>>()

                supervisorScope {
                    projects.forEach { (projectDir, sourceFiles, src) ->
                        if (sourceFiles.isEmpty()) return@forEach

                        val job = launch {
                            val refreshItems = project.readActionSmartCoroutine {
                                val results = mutableListOf<Pair<Source, Path>>()
                                val (pe, rootElements, sources) = project.psiClassesToApt(emptySet())
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

                                    val elements = pe.elementUtils
                                    val dtoFile = project.toDtoFile(projectDir, sourceFile)
                                    try {
                                        val compiler =
                                            AptDtoCompiler(dtoFile, elements, option.defaultNullableInputModifier)
                                        val typeElement: TypeElement =
                                            elements.getTypeElement(compiler.sourceTypeName) ?: return@forEach
                                        val compile = compiler.compile(option.context.getImmutableType(typeElement))
                                        compile.forEach {
                                            DtoGenerator(option.context, DocMetadata(option.context), it).generate()
                                        }
                                        val generatedDir = getGeneratedDir(project, projectDir, src) ?: return@forEach
                                        sources.forEach {
                                            results.add(it to generatedDir)
                                        }
                                    } catch (e: Throwable) {
                                        if (e is ControlFlowException) {
                                            // java.lang.Throwable: Control-flow exceptions (e.g. this class com.intellij.openapi.application.ReadAction$CannotReadException) should never be logged. Instead, these should have been rethrown if caught.
                                            throw e
                                        }
                                        log.error(e)
                                    }
                                }
                                results
                            }
                            needRefresh.addAll(refreshItems)
                        }

                        job.invokeOnCompletion { ex ->
                            if (ex != null && ex !is CancellationException && ex !is ControlFlowException) {
                                log.error(ex)
                            }
                        }
                    }
                }
                asyncRefreshSources(needRefresh)
            }
        }

        suspend fun sourcesProcessKotlin(projects: Set<GenerateProject>) {
            withBackgroundProgress(project, "Processing Kotlin Source") {
                val needRefresh = ConcurrentLinkedQueue<Pair<Source, Path>>()

                supervisorScope {
                    projects.forEach { (projectDir, sourceFiles, src) ->
                        if (sourceFiles.isEmpty()) return@forEach

                        val job = launch {
                            val refreshItems = project.readActionSmartCoroutine {
                                val results = mutableListOf<Pair<Source, Path>>()
                                val ktClasses = sourceFiles.mapNotNull { sourceFile ->
                                    sourceFile.toFile().toPsiFile(project)?.getChildOfType<KtClass>()
                                        ?.takeIf { it.hasJimmerAnnotation() }
                                }.toSet()
                                if (ktClasses.isEmpty()) return@readActionSmartCoroutine emptyList()
                                val generatedDir =
                                    getGeneratedDir(project, projectDir, src)
                                        ?: return@readActionSmartCoroutine emptyList()
                                val (resolver, environment, sources) = project.ktClassesToKsp(
                                    ktClasses
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
                                    org.babyfish.jimmer.ksp.error.ErrorProcessor(
                                        option.context,
                                        option.checkedException
                                    )
                                        .process()
                                    org.babyfish.jimmer.ksp.tuple.TypedTupleProcessor(option.context, emptyList())
                                        .process()
                                    sources.forEach {
                                        results.add(it to generatedDir)
                                    }
                                } catch (e: Throwable) {
                                    if (e is ControlFlowException) {
                                        // java.lang.Throwable: Control-flow exceptions (e.g. this class com.intellij.openapi.application.ReadAction$CannotReadException) should never be logged. Instead, these should have been rethrown if caught.
                                        throw e
                                    }
                                    log.error(e)
                                }
                                results
                            }

                            needRefresh.addAll(refreshItems)
                        }

                        job.invokeOnCompletion { ex ->
                            if (ex != null && ex !is CancellationException && ex !is ControlFlowException) {
                                log.error(ex)
                            }
                        }
                    }
                }
                asyncRefreshSources(needRefresh)
            }
        }

        suspend fun dtoProcessKotlin(projects: Set<GenerateProject>) {
            withBackgroundProgress(project, "Processing Kotlin DTO") {
                val needRefresh = ConcurrentLinkedQueue<Pair<Source, Path>>()

                supervisorScope {
                    projects.forEach { (projectDir, sourceFiles, src) ->
                        if (sourceFiles.isEmpty()) return@forEach

                        val job = launch {
                            val refreshItems = project.readActionSmartCoroutine {
                                val results = mutableListOf<Pair<Source, Path>>()
                                val (resolver, environment, sources) = project.ktClassesToKsp(emptySet())

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
                                            resolver.getClassDeclarationByName(compiler.sourceTypeName)
                                                ?: return@forEach
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
                                            results.add(source to generatedDir)
                                        }
                                    } catch (e: Throwable) {
                                        if (e is ControlFlowException) {
                                            throw e
                                        }
                                        log.error(e)
                                    }
                                }
                                results
                            }
                            needRefresh.addAll(refreshItems)
                        }

                        job.invokeOnCompletion { ex ->
                            if (ex != null && ex !is CancellationException && ex !is ControlFlowException) {
                                log.error(ex)
                            }
                        }
                    }
                }
                asyncRefreshSources(needRefresh)
            }
        }
    }

    private fun Method.invokeWithCatch(obj: Any?, vararg args: Any?): Any? {
        return try {
            invoke(obj, *args)
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }
}
