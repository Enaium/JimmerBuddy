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
import cn.enaium.jimmer.buddy.dialog.panel.ImmutablePropsChoosePanel
import cn.enaium.jimmer.buddy.extensions.template.BuddyTemplateFile
import cn.enaium.jimmer.buddy.utility.*
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiClass
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.core.util.toVirtualFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.borderPanel
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import java.nio.file.Path
import javax.swing.JComponent
import kotlin.io.path.*


/**
 * @author Enaium
 */
class NewDtoFileDialog(
    val project: Project,
    val sourceFile: Path,
    immutableName: String
) : DialogWrapper(false) {

    private val model = NewDtoFileModel()

    val properties = mutableListOf<DtoProperty>()

    init {
        model.immutableName = immutableName
        model.packageName = "${immutableName.substringBeforeLast(".")}.dto"
        model.dtoFileName = immutableName.substringAfterLast(".")
        title = I18n.message("dialog.newDtoFile.title")
        init()
    }

    override fun createCenterPanel(): JComponent {
        return borderPanel {
            addToTop(panel {
                row(I18n.message("dialog.newDtoFile.label.immutableName")) {
                    textField().align(Align.FILL).bindText(model.immutableNameProperty)
                }
                row(I18n.message("dialog.newDtoFile.label.packageName")) {
                    packageChooserField(project, model.packageNameProperty).align(Align.FILL)
                }
                row(I18n.message("dialog.newDtoFile.label.dtoFileName")) {
                    textField().align(Align.FILL).bindText(model.dtoFileNameProperty)
                }
                row(I18n.message("dialog.newDtoFile.label.typeName")) {
                    textField().align(Align.FILL).bindText(model.typeNameProperty)
                }
                row(I18n.message("dialog.newDtoFile.label.modifier")) {
                    JimmerBuddy.Services.UI.segmentedButtonText(this, NewDtoFileModel.Modifier.entries) {
                        it.text
                    }.bind(model.modifierProperty)
                }
                row { label(I18n.message("dialog.newDtoFile.label.chooseProperties")) }
            })
            if (sourceFile.extension == "kt") {
                sourceFile.toFile().toPsiFile(project)?.getChildOfType<KtClass>()?.also {
                    addToCenter(
                        ImmutablePropsChoosePanel(
                            project,
                            thread { runReadOnly { it.toImmutable().toCommonImmutableType() } },
                            properties
                        )
                    )
                }
            } else if (sourceFile.extension == "java") {
                sourceFile.toFile().toVirtualFile()?.findPsiFile(project)?.getChildOfType<PsiClass>()?.also {
                    addToCenter(
                        ImmutablePropsChoosePanel(
                            project,
                            it.toImmutable().toCommonImmutableType(),
                            properties
                        )
                    )
                }
            }
        }
    }

    override fun doOKAction() {
        findProjectDir(sourceFile)?.also { projectDir ->
            val dtoFile = projectDir.resolve("src/main/dto/${model.dtoFileName}.dto")
            val fileTemplateManager = FileTemplateManager.getInstance(project)
            val dtoHeadTemplate = fileTemplateManager.getInternalTemplate(BuddyTemplateFile.JIMMER_DTO_HEAD)
            val dtoHeadContent = dtoHeadTemplate.getText(
                mapOf(
                    "IMMUTABLE_NAME" to model.immutableName,
                    "PACKAGE_NAME" to model.packageName
                )
            )
            val dtoContentTemplate =
                fileTemplateManager.getInternalTemplate(BuddyTemplateFile.JIMMER_DTO_CONTENT)
            val dtoContent = dtoContentTemplate.getText(
                mapOf(
                    "DTO_TYPES" to listOf(
                        mapOf(
                            "modifier" to model.modifier.keyword,
                            "name" to (model.typeName.takeIf { it.isNotBlank() } ?: model.dtoFileName),
                            "properties" to properties
                        )
                    ),
                )
            )
            dtoFile.createParentDirectories()
            if (dtoFile.exists()) {
                val oldDtoContent = dtoFile.readText()
                val oldContent = DiffContentFactory.getInstance().create(oldDtoContent)

                val newContent = DiffContentFactory.getInstance().create("$oldDtoContent\n$dtoContent")
                val diffRequest = SimpleDiffRequest("New DTO", oldContent, newContent, "Old DTO", "New DTO")
                object : DialogWrapper(false) {

                    init {
                        title = "New DTO"
                        init()
                    }

                    override fun createCenterPanel(): JComponent {
                        return DiffManager.getInstance().createRequestPanel(project, {}, null).apply {
                            setRequest(diffRequest)
                        }.component
                    }
                }.showAndGet().ifTrue {
                    dtoFile.writeText("$oldDtoContent\n$dtoContent")
                }
            } else {
                dtoFile.writeText("$dtoHeadContent\n\n$dtoContent")
            }
            JimmerBuddy.getWorkspace(project).asyncRefresh(listOf(dtoFile))
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
        val packageNameProperty = graph.property<String>("")
        val dtoFileNameProperty = graph.property<String>("")
        val typeNameProperty = graph.property<String>("")
        val modifierProperty = graph.property<Modifier>(Modifier.OUTPUT)

        var immutableName by immutableNameProperty

        var packageName by packageNameProperty

        var dtoFileName by dtoFileNameProperty

        var typeName by typeNameProperty

        var modifier by modifierProperty

        enum class Modifier(val text: String, val keyword: String) {
            OUTPUT("View/Output", ""),
            INPUT("Input", "input"),
            SPECIFICATION("Specification", "specification")
        }
    }

    data class DtoProperty(
        val name: String,
        val properties: MutableList<DtoProperty> = mutableListOf()
    )
}