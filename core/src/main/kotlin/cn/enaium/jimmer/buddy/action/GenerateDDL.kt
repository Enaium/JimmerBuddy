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
import cn.enaium.jimmer.buddy.dialog.GenerateDDLDialog
import cn.enaium.jimmer.buddy.utility.*
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
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
class GenerateDDL : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        if (project.isDumb()) return
        val dataContext = e.dataContext
        dataContext.getData(CommonDataKeys.VIRTUAL_FILE)?.also { file ->
            val sourceFile = file.toNioPath().takeIf { it.toFile().isFile } ?: run {
                Notifications.Bus.notify(
                    Notification(
                        JimmerBuddy.INFO_GROUP_ID,
                        "You can only generate DDL from a file",
                        NotificationType.WARNING
                    )
                )
                return
            }
            val commonImmutable = when (sourceFile.extension) {
                "java" -> {
                    sourceFile.toFile().toPsiFile(project)?.getChildOfType<PsiClass>()
                        ?.takeIf { it.isImmutable() }?.toImmutable()?.toCommonImmutableType()
                }

                "kt" -> {
                    sourceFile.toFile().toPsiFile(project)?.getChildOfType<KtClass>()?.takeIf { it.isImmutable() }
                        ?.let { thread { runReadOnly { it.toImmutable().toCommonImmutableType() } } }
                }

                else -> {
                    null
                }
            }

            commonImmutable?.also {
                GenerateDDLDialog(project, it).show()
            }
        }
    }

    override fun update(e: AnActionEvent) {
        e.visibleWithImmutable()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}