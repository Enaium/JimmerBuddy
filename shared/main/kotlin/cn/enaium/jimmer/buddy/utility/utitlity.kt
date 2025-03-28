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

import com.intellij.compiler.CompilerConfiguration
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name


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

    OrderEnumerator.orderEntries(this).runtimeOnly().classesRoots.forEach {
        if (it.name.startsWith("jimmer-core-kotlin")) {
            return true
        }
    }
    return false
}