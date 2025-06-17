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

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import kotlin.io.path.extension

/**
 * @author Enaium
 */
fun AnActionEvent.visibleWithImmutable() {
    val project = this.project ?: return
    val dataContext = this.dataContext
    dataContext.getData(CommonDataKeys.VIRTUAL_FILE)?.toNioPath()?.takeIf { it.toFile().isFile }
        ?.also { sourceFile ->
            this.presentation.isVisible = when (sourceFile.extension) {
                "java" -> {
                    sourceFile.toFile().toPsiFile(project)?.getChildOfType<PsiClass>()
                        ?.takeIf { it.isImmutable() } != null
                }

                "kt" -> {
                    sourceFile.toFile().toPsiFile(project)?.getChildOfType<KtClass>()
                        ?.takeIf { it.isImmutable() } != null
                }

                else -> {
                    false
                }
            }
        } ?: run {
        this.presentation.isVisible = false
    }
}