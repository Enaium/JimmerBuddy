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

import cn.enaium.jimmer.buddy.extensions.gradle.ksp.KspData
import com.intellij.compiler.CompilerConfiguration
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.util.Computable
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.squareup.javapoet.TypeName
import org.babyfish.jimmer.Formula
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.Scalar
import org.babyfish.jimmer.error.ErrorFamily
import org.babyfish.jimmer.error.ErrorField
import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.Embeddable
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.IdView
import org.babyfish.jimmer.sql.JoinColumn
import org.babyfish.jimmer.sql.JoinTable
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.LogicalDeleted
import org.babyfish.jimmer.sql.ManyToMany
import org.babyfish.jimmer.sql.ManyToManyView
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.MappedSuperclass
import org.babyfish.jimmer.sql.OneToMany
import org.babyfish.jimmer.sql.OneToOne
import org.babyfish.jimmer.sql.Serialized
import org.babyfish.jimmer.sql.Version
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.plugins.gradle.util.GradleUtil
import java.io.File
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.jvm.Transient


/**
 * @author Enaium
 */
data class Source(
    val packageName: String,
    val fileName: String,
    val extensionName: String,
    val content: String,
)

fun findProjectDir(file: Path): Path? {
    var current: Path? = file.parent
    while (current != null) {
        if (isProject(current)) {
            return current
        }
        current = current.parent
    }
    return null
}

fun isProject(path: Path): Boolean {
    return listOf("build.gradle.kts", "build.gradle", "pom.xml", ".git").any { path.resolve(it).exists() }
}

fun isMavenProject(path: Path): Boolean {
    return path.resolve("pom.xml").exists()
}

fun isGradleProject(path: Path): Boolean {
    return path.resolve("build.gradle.kts").exists() || path.resolve("build.gradle").exists()
}

fun isGeneratedFile(path: Path): Boolean {
    var current: Path? = path.parent
    while (current != null) {
        if (current.name == "generated") {
            return true
        }
        current = current.parent
    }
    return false
}

fun findProjects(rootProject: Path): Set<Path> {
    val results = mutableSetOf<Path>()
    findProjects(rootProject, results)
    return results
}

private fun findProjects(rootProject: Path, results: MutableSet<Path>, level: Int = 0) {
    if (isProject(rootProject)) {
        results.add(rootProject)
    }
    rootProject.toFile().listFiles()?.forEach {
        val file = it.toPath()
        if (file.isDirectory() && isProject(file)) {
            results.add(file)
            if (level < 4)
                findProjects(file, results, level + 1)
        }
    }
}

fun <T> copyOnWriteSetOf(vararg elements: T): CopyOnWriteArraySet<T> {
    return CopyOnWriteArraySet<T>().apply {
        addAll(elements)
    }
}

fun <T> runReadOnly(block: () -> T): T {
    return ApplicationManager.getApplication().runReadAction(Computable {
        return@Computable block()
    })
}

fun <T> runWriteOnly(block: () -> T): T {
    return ApplicationManager.getApplication().runWriteAction(Computable {
        return@Computable block()
    })
}

fun <T> thread(block: () -> T): T {
    return ApplicationManager.getApplication().executeOnPooledThread(Callable {
        return@Callable block()
    }).get()
}

fun invokeLater(block: () -> Unit) {
    ApplicationManager.getApplication().invokeLater {
        block()
    }
}

fun isK2Enable(): Boolean {
    return System.getProperty("idea.kotlin.plugin.use.k2")?.toBoolean() == true
}

fun Project.isDumb(): Boolean {
    return isDisposed || DumbService.isDumb(this)
}

fun Project.isJimmerProject(): Boolean {
    return isJavaProject() || isKotlinProject()
}

fun Project.isJavaProject(): Boolean {
    if (isDisposed) {
        return false
    }

    return modules.any { module ->
        CompilerConfiguration.getInstance(this).getAnnotationProcessingConfiguration(module).processorPath.split(
            File.pathSeparator
        ).any { it.contains("jimmer-apt") }
    }
}

fun Project.isKotlinProject(): Boolean {
    if (isDisposed) {
        return false
    }

    thread { OrderEnumerator.orderEntries(this).runtimeOnly().classesRoots }.forEach {
        if (it.name.startsWith("jimmer-core-kotlin")) {
            return true
        }
    }
    return false
}

fun Project.runWhenSmart(block: () -> Unit) {
    DumbService.getInstance(this).runWhenSmart {
        block()
    }
}

fun Project.runReadActionSmart(block: () -> Unit) {
    DumbService.getInstance(this).runReadActionInSmartMode {
        block()
    }
}

fun Project.runWriteActionSmart(block: () -> Unit) {
    DumbService.getInstance(this).runWhenSmart {

    }
}

val toManyAnnotations = listOfNotNull(OneToMany::class.qualifiedName, ManyToMany::class.qualifiedName)
val toOneAnnotations = listOfNotNull(OneToOne::class.qualifiedName, ManyToOne::class.qualifiedName)

val mappedByAnnotations = toManyAnnotations + listOfNotNull(OneToOne::class.qualifiedName)

fun getKspOptions(project: Project): Map<String, String> {
    ModuleManager.getInstance(project).modules.forEach {
        val data =
            GradleUtil.findGradleModuleData(it)?.children?.find { it.key == KspData.KEY }?.data as? KspData
        if (data != null) {
            val options = data.arguments
            return options
        }
    }
    return emptyMap()
}

fun TypeName.simplify(): String {
    return this.toString().simplifyTypeName()
}

fun com.squareup.kotlinpoet.TypeName.simplify(): String {
    return this.toString().simplifyTypeName()
}

private fun String.simplifyTypeName(): String {
    fun splitGenericParams(params: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var depth = 0
        for (char in params) {
            when (char) {
                '<' -> {
                    depth++
                    current.append(char)
                }

                '>' -> {
                    depth--
                    current.append(char)
                }

                ',' -> {
                    if (depth == 0) {
                        result.add(current.toString().trim())
                        current.clear()
                    } else {
                        current.append(char)
                    }
                }

                else -> current.append(char)
            }
        }
        if (current.isNotEmpty()) {
            result.add(current.toString().trim())
        }
        return result
    }

    fun stripFullType(typeStr: String): String {
        val trimmed = typeStr.trim()
        val genericStart = trimmed.indexOf('<')
        return if (genericStart == -1) {
            trimmed.substringAfterLast('.')
        } else {
            val base = trimmed.substring(0, genericStart).substringAfterLast('.')
            val genericContent = trimmed.substring(genericStart + 1, trimmed.lastIndexOf('>'))
            val params = splitGenericParams(genericContent).map { stripFullType(it) }
            "$base<${params.joinToString(", ")}>"
        }
    }

    return stripFullType(this)
}

fun String.toHtml(): String {
    val flavour = CommonMarkFlavourDescriptor()
    val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(this)
    return HtmlGenerator(this, parsedTree, flavour).generateHtml()
}

fun Project.findPsiClass(name: String): PsiClass? {
    return JavaPsiFacade.getInstance(this)
        .findClass(name, this.allScope())
}

fun Project.findKtClass(name: String): KtClass? {
    return KotlinFullClassNameIndex[name, this, this.allScope()].firstOrNull() as? KtClass
}

val jimmerAnnotations = listOf(
    Immutable::class,
    Entity::class,
    MappedSuperclass::class,
    Embeddable::class,
    ErrorFamily::class,
    ErrorField::class,
    Id::class,
    IdView::class,
    Key::class,
    Version::class,
    Formula::class,
    OneToOne::class,
    OneToMany::class,
    ManyToOne::class,
    ManyToMany::class,
    ManyToManyView::class,
    Column::class,
    GeneratedValue::class,
    JoinColumn::class,
    JoinTable::class,
    Transient::class,
    Serialized::class,
    LogicalDeleted::class,
    Scalar::class
)