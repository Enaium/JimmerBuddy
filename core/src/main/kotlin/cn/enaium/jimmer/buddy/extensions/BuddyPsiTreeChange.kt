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

package cn.enaium.jimmer.buddy.extensions

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.JimmerBuddy.GenerateProject
import cn.enaium.jimmer.buddy.storage.JimmerBuddySetting
import cn.enaium.jimmer.buddy.utility.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
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
        if (!JimmerBuddySetting.INSTANCE.state.autoGenerate) {
            return
        }
        val path = try {
            psiFile.virtualFile.toNioPath()
        } catch (_: Throwable) {
            return
        }
        isGeneratedFile(path) && return
        listOf("java", "kt", "dto").any { path.extension == it }.not() && return
        JimmerBuddy.DEQ.schedule("PsiChange") {
            if (project.isJavaProject() && !project.isDumb()) {
                JimmerBuddy.getWorkspace(project).also {
                    if (path.extension == "java" && runReadOnly {
                            psiFile.getChildOfType<PsiClass>()?.hasJimmerAnnotation()
                        } == true) {
                        it.sourcesProcessJava(
                            GenerateProject.generate(
                                path,
                                GenerateProject.SourceRootType.JAVA
                            )
                        )
                    } else if (path.extension == "dto") {
                        it.dtoProcessJava(
                            GenerateProject.generate(
                                path,
                                GenerateProject.SourceRootType.DTO
                            )
                        )
                    }
                }
            } else if (project.isKotlinProject() && !project.isDumb()) {
                JimmerBuddy.getWorkspace(project).also {
                    if (path.extension == "kt" && runReadOnly {
                            psiFile.getChildOfType<KtClass>()?.hasJimmerAnnotation()
                        } == true) {
                        it.sourcesProcessKotlin(
                            GenerateProject.generate(
                                path,
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
                    } else if (path.extension == "dto") {
                        it.dtoProcessKotlin(
                            GenerateProject.generate(
                                path,
                                GenerateProject.SourceRootType.DTO
                            )
                        )
                    }
                }
            }
        }
    }
}