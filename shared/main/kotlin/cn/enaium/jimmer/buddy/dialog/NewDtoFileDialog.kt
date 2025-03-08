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

package cn.enaium.jimmer.buddy.dialog

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.dialog.panel.KotlinPropsChoosePanel
import cn.enaium.jimmer.buddy.template.JimmerProjectTemplateFile
import cn.enaium.jimmer.buddy.utility.findProjectDir
import cn.enaium.jimmer.buddy.utility.toImmutableType
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.borderPanel
import java.nio.file.Path
import javax.swing.JComponent
import kotlin.io.path.createParentDirectories
import kotlin.io.path.extension
import kotlin.io.path.writeText

/**
 * @author Enaium
 */
class NewDtoFileDialog(
    val project: Project,
    val sourceFile: Path,
    immutableName: String
) : DialogWrapper(false) {

    private val model = NewDtoFileModel()

    init {
        model.immutable = immutableName
        model.dtoFileName = immutableName.substringAfterLast(".")
        title = "New DTO File"
        isResizable = false
        init()
    }

    override fun createCenterPanel(): JComponent {
        return borderPanel {
            addToTop(panel {
                row("Immutable Name:") {
                    textField().bindText(model.immutableNameProperty)
                }
                row("DTO File Name:") {
                    textField().bindText(model.dtoFileNameProperty)
                }
                row { label("Choose Properties") }
            })
            if (sourceFile.extension == "kt") {
                sourceFile.toFile().toPsiFile(project)?.getChildOfType<KtClass>()?.also {
                    addToCenter(KotlinPropsChoosePanel(it.toImmutableType()))
                }
            }
        }
    }

    override fun doOKAction() {
        findProjectDir(sourceFile)?.also { projectDir ->
            val dtoFile = projectDir.resolve("src/main/dto/${model.dtoFileName}.dto")
            val fileTemplateManager = FileTemplateManager.getInstance(project)
            val dtoTemplate = fileTemplateManager.getInternalTemplate(JimmerProjectTemplateFile.JIMMER_DTO)
            val dtoContent = dtoTemplate.getText(
                mapOf(
                    "IMMUTABLE_NAME" to model.immutable,
                    "PACKAGE_NAME" to "${model.immutable.substringBeforeLast(".")}.dto"
                )
            )
            dtoFile.createParentDirectories()
            dtoFile.writeText(dtoContent)
            JimmerBuddy.asyncRefresh(listOf(dtoFile))
        } ?: Notifications.Bus.notify(
            Notification(
                JimmerBuddy.INFO_GROUP_ID,
                "Can't find project directory",
                NotificationType.WARNING
            )
        )
        super.doOKAction()
    }

    private class NewDtoFileModel : BaseState() {
        private val graph: PropertyGraph = PropertyGraph()

        val immutableNameProperty = graph.property<String>("")
        val dtoFileNameProperty = graph.property<String>("")

        var immutable by immutableNameProperty

        var dtoFileName by dtoFileNameProperty
    }
}