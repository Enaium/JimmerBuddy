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

import cn.enaium.jimmer.buddy.extensions.wizard.JimmerProjectModel
import cn.enaium.jimmer.buddy.utility.*
import com.google.devtools.ksp.getClassDeclarationByName
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.babyfish.jimmer.apt.client.DocMetadata
import org.babyfish.jimmer.apt.createContext
import org.babyfish.jimmer.apt.dto.AptDtoCompiler
import org.babyfish.jimmer.apt.dto.DtoGenerator
import org.babyfish.jimmer.apt.entry.EntryProcessor
import org.babyfish.jimmer.apt.error.ErrorProcessor
import org.babyfish.jimmer.apt.immutable.ImmutableProcessor
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType
import org.babyfish.jimmer.dto.compiler.DtoFile
import org.babyfish.jimmer.dto.compiler.DtoModifier
import org.babyfish.jimmer.dto.compiler.OsFile
import org.babyfish.jimmer.ksp.Context
import org.babyfish.jimmer.ksp.KspDtoCompiler
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.core.util.toVirtualFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import java.io.Reader
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
    val PROJECT_MODEL_PROP_KEY = Key<GraphProperty<JimmerProjectModel>>("jimmer_project_model")

    object Icons {
        val LOGO = IconLoader.getIcon("/icons/logo.svg", JimmerBuddy::class.java)
        val LOGO_NORMAL = IconLoader.getIcon("/icons/normal.svg", JimmerBuddy::class.java)
        val IMMUTABLE = IconLoader.getIcon("/icons/immutable.svg", JimmerBuddy::class.java)
        val PROP = IconLoader.getIcon("/icons/prop.svg", JimmerBuddy::class.java)
        val DTO = IconLoader.getIcon("/icons/dto.svg", JimmerBuddy::class.java)

        object Database {
            val DB = IconLoader.getIcon("/icons/database/dbms.svg", JimmerBuddy::class.java)
            val TABLE = IconLoader.getIcon("/icons/database/table.svg", JimmerBuddy::class.java)
            val COLUMN = IconLoader.getIcon("/icons/database/column.svg", JimmerBuddy::class.java)
            val COLUMN_GOLD_KEY = IconLoader.getIcon("/icons/database/columnGoldKey.svg", JimmerBuddy::class.java)
            val COLUMN_BLUE_KEY = IconLoader.getIcon("/icons/database/columnBlueKey.svg", JimmerBuddy::class.java)
            val INDEX = IconLoader.getIcon("/icons/database/index.svg", JimmerBuddy::class.java)
        }
    }

    const val INFO_GROUP_ID = "JimmerBuddy.NotificationGroup"

    val DEQ = DelayedExecutionQueue(2000)

    val PSI_SHARED: PsiShared = ServiceLoader.load(PsiShared::class.java, JimmerBuddy::class.java.classLoader).first()

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
            suspend fun generate(projects: Set<Path>, srcSets: Set<String>, language: Language): Set<GenerateProject> =
                withContext(Dispatchers.IO) {
                    projects.map { project ->
                        srcSets.map { src ->
                            GenerateProject(
                                projectDir = project,
                                project.resolve("src/${src}/${language.dir}").walk()
                                    .filter { it.extension == language.extension }.toSet(),
                                src
                            )
                        }
                    }.flatten().toSet()
                }

            suspend fun generate(sourceFile: Path, language: Language): Set<GenerateProject> =
                withContext(Dispatchers.IO) {
                    setOfNotNull(findProjectDir(sourceFile)?.let { project ->
                        project.resolve("src").toFile().walk().maxDepth(1)
                            .find { sourceFile.startsWith(it.toPath().resolve(language.dir)) }?.name?.let { src ->
                                GenerateProject(project, setOf(sourceFile), src)
                            }
                    })
                }
        }

        enum class Language(val dir: String, val extension: String) {
            JAVA("java", "java"),
            KOTLIN("kotlin", "kt"),
            DTO("dto", "dto")
        }


        override fun hashCode(): Int {
            return projectDir.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as GenerateProject

            if (projectDir != other.projectDir) return false
            if (src != other.src) return false

            return true
        }
    }

    class Workspace(val project: Project) {

        val log = Log(project)

        private val javaImmutablePsiClassCache = CopyOnWriteArraySet<PsiClass>()

        private val kotlinImmutableKtClassCache = CopyOnWriteArraySet<KtClass>()

        var isInit = false

        fun init() {
            if (isInit) return
            isInit = true
            project.runWhenSmart {
                val projects = findProjects(project.guessProjectDir()?.toNioPath()!!)
                CoroutineScope(Dispatchers.Default).launch {
                    log.info("Project ${project.name} is initializing")
                    if (project.isJavaProject()) {
                        sourcesProcessJava(
                            GenerateProject.generate(
                                projects,
                                setOf("main", "test"),
                                GenerateProject.Language.JAVA
                            )
                        )
                        dtoProcessJava(
                            GenerateProject.generate(
                                projects,
                                setOf("main", "test"),
                                GenerateProject.Language.DTO
                            )
                        )
                    } else if (project.isKotlinProject()) {
                        sourceProcessKotlin(
                            GenerateProject.generate(
                                projects,
                                setOf("main", "test"),
                                GenerateProject.Language.KOTLIN
                            )
                        )
                        dtoProcessKotlin(
                            GenerateProject.generate(
                                projects,
                                setOf("main", "test"),
                                GenerateProject.Language.DTO
                            )
                        )
                    }
                    log.info("Project ${project.name} is initialized")
                }
            }
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

        private fun getGeneratedDir(project: Project, projectDir: Path, src: String): Path? {
            if (project.isJavaProject()) {
                return if (isMavenProject(projectDir)) {
                    projectDir.resolve("target/generated-sources/annotations")
                } else if (isGradleProject(projectDir)) {
                    projectDir.resolve("build/generated/sources/annotationProcessor/java/${src}")
                } else {
                    null
                }
            } else if (project.isKotlinProject()) {
                return if (isGradleProject(projectDir)) {
                    projectDir.resolve("build/generated/ksp/${src}/kotlin")
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
            invokeLater {
                try {
                    LocalFileSystem.getInstance().refreshNioFiles(files)
                    log.info("Refreshed ${files.joinToString(", ") { it.name }}")
                } catch (_: Throwable) {
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
            log.info("SourcesProcessJava Project:${projects.joinToString { "${it.projectDir.name}:${it.src}" }}")
            val needRefresh = mutableListOf<Pair<Source, Path>>()
            projects.forEach { (projectDir, sourceFiles, src) ->
                sourceFiles.isEmpty() && return@forEach
                val psiCaches = CopyOnWriteArraySet<PsiClass>()
                project.runReadActionSmart {
                    sourceFiles.forEach {
                        val psiFile = it.toFile().toVirtualFile()?.findPsiFile(project) ?: return@forEach
                        psiFile.getChildOfType<PsiClass>()?.takeIf { it.hasJimmerAnnotation() }?.also {
                            psiCaches.add(it)
                        }
                    }

                    log.info("SourcesProcessJava Project:${projectDir.name} PsiCaches:${javaImmutablePsiClassCache.size}")

                    val generatedDir = getGeneratedDir(project, projectDir, src) ?: return@runReadActionSmart

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
                        val immutableProcessor = ImmutableProcessor(context, pe.messager)
                        val immutableTypeElements = try {
                            ImmutableProcessor::class.java.getDeclaredMethod(
                                "parseImmutableTypes",
                                RoundEnvironment::class.java
                            )
                        } catch (_: Throwable) {
                            null
                        }?.let {
                            it.isAccessible = true
                            it.invoke(immutableProcessor, roundEnv) as Map<TypeElement, ImmutableType>
                        } ?: emptyMap()

                        ImmutableProcessor::class.java.declaredMethods.find { it.name == "generateJimmerTypes" }?.also {
                            it.isAccessible = true
                            it.invoke(
                                immutableProcessor,
                                immutableTypeElements.filter { ite ->
                                    psiCaches.mapNotNull { it.qualifiedName }.any { it == ite.value.qualifiedName }
                                })
                        }

                        EntryProcessor(context, immutableTypeElements.keys).process()
                        ErrorProcessor(context, false).process(roundEnv)
                        sources.forEach {
                            needRefresh.add(it to generatedDir)
                        }
                    } catch (e: Throwable) {
                        log.error(e)
                    }
                }
                javaImmutablePsiClassCache.addAll(psiCaches)
            }
            asyncRefreshSources(needRefresh)
        }

        suspend fun dtoProcessJava(projects: Set<GenerateProject>) {
            log.info("DtoProcessJava Project:${projects.joinToString { "${it.projectDir.name}:${it.src}" }}")
            val needRefresh = mutableListOf<Pair<Source, Path>>()
            project.runReadActionSmart {
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
                projects.forEach { (projectDir, sourceFiles, src) ->
                    sourceFiles.forEach {
                        sourceFiles.isEmpty() && return@forEach
                        val elements = pe.elementUtils
                        val dtoFile = DtoFile(object : OsFile {
                            override fun getAbsolutePath(): String {
                                return it.absolutePathString()
                            }

                            override fun openReader(): Reader {
                                return it.readText().reader()
                            }
                        }, projectDir.absolutePathString(), "", emptyList(), it.name)
                        val compiler = AptDtoCompiler(dtoFile, elements, DtoModifier.STATIC)
                        val typeElement: TypeElement =
                            elements.getTypeElement(compiler.sourceTypeName) ?: return@forEach
                        try {
                            val compile = compiler.compile(context.getImmutableType(typeElement))
                            compile.forEach {
                                DtoGenerator(context, DocMetadata(context), it).generate()
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

        suspend fun sourceProcessKotlin(projects: Set<GenerateProject>) {
            val needRefresh = mutableListOf<Pair<Source, Path>>()
            log.info("SourceProcessKotlin Project:${projects.joinToString { "${it.projectDir.name}:${it.src}" }}")
            projects.forEach { (projectDir, sourceFiles, src) ->
                sourceFiles.isEmpty() && return@forEach
                val ktClassCaches = CopyOnWriteArraySet<KtClass>()
                project.runReadActionSmart {
                    sourceFiles.forEach {
                        val psiFile = it.toFile().toPsiFile(project) ?: return@forEach
                        psiFile.getChildOfType<KtClass>()?.takeIf { it.hasJimmerAnnotation() }?.also {
                            ktClassCaches.add(it)
                        }
                    }

                    log.info("SourceProcessKotlin Project:${projectDir.name} KtClassCaches:${kotlinImmutableKtClassCache.size}")

                    val generatedDir = getGeneratedDir(project, projectDir, src) ?: return@runReadActionSmart

                    val (resolver, environment, sources) = ktClassToKsp(ktClassCaches, kotlinImmutableKtClassCache)
                    try {
                        val context = Context(resolver, environment)
                        org.babyfish.jimmer.ksp.immutable.ImmutableProcessor(context, false).process()
                        org.babyfish.jimmer.ksp.error.ErrorProcessor(context, true).process()
                        sources.forEach {
                            needRefresh.add(it to generatedDir)
                        }
                    } catch (e: Throwable) {
                        log.error(e)
                    }
                }
                kotlinImmutableKtClassCache.addAll(ktClassCaches)
            }
            asyncRefreshSources(needRefresh)
        }

        suspend fun dtoProcessKotlin(projects: Set<GenerateProject>) {
            val needRefresh = mutableListOf<Pair<Source, Path>>()
            log.info("DtoProcessKotlin Project:${projects.joinToString { "${it.projectDir.name}:${it.src}" }}")
            project.runReadActionSmart {
                val (resolver, environment, sources) = ktClassToKsp(
                    CopyOnWriteArraySet(),
                    kotlinImmutableKtClassCache
                )
                val context = Context(resolver, environment)
                projects.forEach { (projectDir, sourceFiles, src) ->
                    sourceFiles.isEmpty() && return@forEach
                    sourceFiles.forEach {
                        val dtoFile = DtoFile(object : OsFile {
                            override fun getAbsolutePath(): String {
                                return it.absolutePathString()
                            }

                            override fun openReader(): Reader {
                                return it.readText().reader()
                            }
                        }, projectDir.absolutePathString(), "", emptyList(), it.name)
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
}