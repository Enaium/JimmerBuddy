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
import cn.enaium.jimmer.buddy.dialog.NewDtoFileDialog
import cn.enaium.jimmer.buddy.utility.hasJimmerAnnotation
import cn.enaium.jimmer.buddy.utility.runReadOnly
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import java.util.concurrent.Callable
import kotlin.io.path.extension

/**
 * @author Enaium
 */
class NewDtoFile : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        if (DumbService.isDumb(project)) return
        val dataContext = e.dataContext
        dataContext.getData(CommonDataKeys.VIRTUAL_FILE)?.also { file ->
            val sourceFile = file.toNioPath().takeIf { it.toFile().isFile } ?: run {
                Notifications.Bus.notify(
                    Notification(
                        JimmerBuddy.INFO_GROUP_ID,
                        "You can only create a new DTO file from a file",
                        NotificationType.WARNING
                    )
                )
                return
            }

            ApplicationManager.getApplication().executeOnPooledThread(Callable {
                if (sourceFile.extension == "java") {
                    runReadOnly {
                        sourceFile.toFile().toPsiFile(project)?.getChildOfType<PsiClass>()
                            ?.takeIf { it.hasJimmerAnnotation() }?.qualifiedName
                    }
                } else if (sourceFile.extension == "kt") {
                    runReadOnly {
                        sourceFile.toFile().toPsiFile(project)?.getChildOfType<KtClass>()
                            ?.takeIf { it.hasJimmerAnnotation() }?.fqName?.asString()
                    }
                } else {
                    null
                }
            }).get()?.also { name ->
                NewDtoFileDialog(project, sourceFile, name).show()
            } ?: Notifications.Bus.notify(
                Notification(
                    JimmerBuddy.INFO_GROUP_ID,
                    "You selected file is not a immutable type",
                    NotificationType.WARNING
                )
            )
        }
    }
}