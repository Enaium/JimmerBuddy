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

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

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