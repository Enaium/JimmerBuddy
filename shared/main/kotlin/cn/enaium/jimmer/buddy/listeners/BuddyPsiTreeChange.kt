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

package cn.enaium.jimmer.buddy.listeners

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.utility.findProjectDir
import cn.enaium.jimmer.buddy.utility.isGeneratedFile
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import kotlin.io.path.extension

/**
 * @author Enaium
 */
class BuddyPsiTreeChange(val project: Project) : PsiTreeChangeAdapter() {
    override fun childAdded(event: PsiTreeChangeEvent) {
        event.file?.also { onChange(it) }
    }

    override fun childRemoved(event: PsiTreeChangeEvent) {
        event.file?.also { onChange(it) }
    }

    override fun childReplaced(event: PsiTreeChangeEvent) {
        event.file?.also { onChange(it) }
    }

    override fun childrenChanged(event: PsiTreeChangeEvent) {
        event.file?.also { onChange(it) }
    }

    override fun childMoved(event: PsiTreeChangeEvent) {
        event.file?.also { onChange(it) }
    }

    override fun propertyChanged(event: PsiTreeChangeEvent) {
        event.file?.also { onChange(it) }
    }

    private fun onChange(psiFile: PsiFile) {
        val path = try {
            psiFile.virtualFile.toNioPath()
        } catch (_: Throwable) {
            return
        }
        isGeneratedFile(path) && return
        listOf<String>("java", "kt").any { path.extension == it }.not() && return
        JimmerBuddy.DEQ.schedule("PsiChange") {
            if (JimmerBuddy.isJavaProject(project)) {
                JimmerBuddy.init()
                JimmerBuddy.sourcesProcessJava(
                    project,
                    mapOf((findProjectDir(path) ?: return@schedule) to listOf(path).filter { it.extension == "java" }
                        .also { if (it.isEmpty()) return@schedule })
                )
            } else if (JimmerBuddy.isKotlinProject(project)) {
                JimmerBuddy.init()
                JimmerBuddy.sourceProcessKotlin(
                    project,
                    mapOf((findProjectDir(path) ?: return@schedule) to listOf(path).filter { it.extension == "kt" }
                        .also { if (it.isEmpty()) return@schedule })
                )
            }
        }
    }
}