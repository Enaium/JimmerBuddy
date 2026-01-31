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

package cn.enaium.jimmer.buddy.action

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.JimmerBuddy.GenerateProject
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiFile
import cn.enaium.jimmer.buddy.storage.JimmerBuddySetting
import cn.enaium.jimmer.buddy.utility.*
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import kotlin.io.path.extension

/**
 * @author Enaium
 */
class Generate : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        if (project.isDumb()) return
        val dataContext = e.dataContext
        dataContext.getData(CommonDataKeys.VIRTUAL_FILE)?.also { file ->

            val path = try {
                file.toNioPath()
            } catch (_: Throwable) {
                return
            }
            CoroutineScope(Dispatchers.IO).launch {
                when (path.extension) {
                    "java" -> {
                        JimmerBuddy.getWorkspace(project).also {
                            if (runReadOnly {
                                    file.toPsiFile(project)?.getChildOfType<PsiClass>()?.hasJimmerAnnotation()
                                } == true) {
                                it.sourcesProcessJava(
                                    GenerateProject.generate(
                                        path,
                                        GenerateProject.SourceRootType.JAVA
                                    )
                                )
                            }
                        }
                    }

                    "kt" -> {
                        JimmerBuddy.getWorkspace(project).also {
                            if (runReadOnly {
                                    file.toPsiFile(project)?.getChildOfType<KtClass>()?.hasJimmerAnnotation()
                                } == true) {
                                it.sourcesProcessKotlin(
                                    GenerateProject.generate(
                                        path,
                                        listOf(
                                            GenerateProject.SourceRootType.KOTLIN,
                                            GenerateProject.SourceRootType.JAVA_KOTLIN
                                        )
                                    )
                                )
                            }
                        }
                    }

                    "dto" -> {
                        JimmerBuddy.getWorkspace(project).also {
                            if (it.isJavaProject) {
                                it.dtoProcessJava(
                                    GenerateProject.generate(
                                        path,
                                        GenerateProject.SourceRootType.DTO
                                    )
                                )
                            } else if (it.isKotlinProject) {
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
    }

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        e.visibleWithImmutable()
        if (!e.presentation.isVisible) {
            e.presentation.isVisible =
                e.dataContext.getData(CommonDataKeys.VIRTUAL_FILE)?.toPsiFile(project) is DtoPsiFile
        }
        e.presentation.isEnabledAndVisible = !JimmerBuddySetting.INSTANCE.state.autoGenerate
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}