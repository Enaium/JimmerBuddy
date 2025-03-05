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

package cn.enaium.jimmer.buddy.wizard

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.utitlity.segmentedButtonText
import com.intellij.ide.IdeBundle
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.joinCanonicalPath
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.ui.BrowseFolderDescriptor.Companion.withPathToTextConvertor
import com.intellij.openapi.ui.BrowseFolderDescriptor.Companion.withTextToPathConvertor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.getCanonicalPath
import com.intellij.openapi.ui.getPresentablePath
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.UIBundle
import com.intellij.ui.dsl.builder.*
import java.io.File

/**
 * @author Enaium
 */
class JimmerProjectPanel(propertyGraph: PropertyGraph, private val wizardContext: WizardContext, builder: Panel) {

    private val projectModel = JimmerProjectModel()

    private val entityNameProperty = propertyGraph.lazyProperty(::suggestName)
    private val locationProperty = propertyGraph.lazyProperty(::suggestLocationByName)
    private val canonicalPathProperty = locationProperty.joinCanonicalPath(entityNameProperty)

    init {
        builder.panel {
            row("Name:") { textField().bindText(entityNameProperty) }
            row("Location:") {
                val commentLabel =
                    projectLocationField(locationProperty, wizardContext)
                        .align(AlignX.FILL)
                        .comment(getLocationComment(), 100)
                        .comment!!
                entityNameProperty.afterChange {
                    commentLabel.text = getLocationComment()
                    updateModel()
                }
                locationProperty.afterChange {
                    commentLabel.text = getLocationComment()
                    entityNameProperty.set(suggestName(entityNameProperty.get()))
                    updateModel()
                }
            }
            row("Artifact:") {
                textField().bindText(projectModel.artifactProperty)
                projectModel.artifactProperty.afterChange {
                    updateModel()
                }
            }
            row("Group:") {
                textField().bindText(projectModel.groupProperty)
                projectModel.groupProperty.afterChange {
                    updateModel()
                }
            }
            row("Type:") {
                segmentedButtonText(JimmerProjectModel.Type.entries) {
                    it.text
                }.bind(projectModel.typeProperty)
                projectModel.typeProperty.afterChange {
                    updateModel()
                }
            }
            row("Language:") {
                segmentedButtonText(JimmerProjectModel.Language.entries) {
                    it.text
                }.bind(projectModel.languageProperty)
                projectModel.languageProperty.afterChange {
                    updateModel()
                }
            }
            row("Builder:") {
                segmentedButtonText(JimmerProjectModel.Builder.entries) {
                    it.text
                }.bind(projectModel.builderProperty)
                projectModel.builderProperty.afterChange {
                    updateModel()
                }
            }
            row("Wrapper Version:") {
                textField().bindText(projectModel.wrapperVersionProperty)
                projectModel.wrapperVersionProperty.afterChange {
                    updateModel()
                }
            }
            row {
                text("For more configuration options, visit <a href=\"https://babyfish-ct.github.io/jimmer-doc/docs/quick-view/get-started/create-project\">Jimmer Document</a>")
            }
        }
        updateModel()
    }

    private fun suggestName(): String {
        return suggestName("untitled")
    }

    private fun suggestName(prefix: String): String {
        val projectFileDirectory = File(wizardContext.projectFileDirectory)
        return FileUtil.createSequentFileName(projectFileDirectory, prefix, "")
    }

    private fun suggestLocationByName(): String {
        return wizardContext.projectFileDirectory
    }

    private fun getLocationComment(): String {
        val shortPath = StringUtil.shortenPathWithEllipsis(getPresentablePath(canonicalPathProperty.get()), 60)
        return UIBundle.message(
            "label.project.wizard.new.project.path.description",
            wizardContext.isCreatingNewProjectInt,
            shortPath,
        )
    }

    private fun updateModel() {
        wizardContext.setProjectFileDirectory(canonicalPathProperty.get())
        wizardContext.projectName = entityNameProperty.get()
        wizardContext.defaultModuleName = entityNameProperty.get()
        wizardContext.getUserData(JimmerBuddy.PROJECT_MODEL_PROP_KEY)?.set(projectModel)
    }

    private fun Row.projectLocationField(
        locationProperty: GraphProperty<String>,
        wizardContext: WizardContext,
    ): Cell<TextFieldWithBrowseButton> {
        val fileChooserDescriptor =
            FileChooserDescriptorFactory.createSingleLocalFileDescriptor()
                .withFileFilter { it.isDirectory }
                .withPathToTextConvertor(::getPresentablePath)
                .withTextToPathConvertor(::getCanonicalPath)
        val title = IdeBundle.message("title.select.project.file.directory", wizardContext.presentationName)
        val property = locationProperty.transform(::getPresentablePath, ::getCanonicalPath)
        return textFieldWithBrowseButton(title, wizardContext.project, fileChooserDescriptor).bindText(property)
    }
}