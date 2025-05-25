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

import com.intellij.icons.AllIcons
import com.intellij.ide.IdeBundle
import com.intellij.ide.util.PackageChooserDialog
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.util.bind
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.BrowseFolderDescriptor.Companion.withPathToTextConvertor
import com.intellij.openapi.ui.BrowseFolderDescriptor.Companion.withTextToPathConvertor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.addExtension
import com.intellij.openapi.ui.getCanonicalPath
import com.intellij.openapi.ui.getPresentablePath
import com.intellij.ui.components.fields.ExpandableTextField
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindText
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo

/**
 * @author Enaium
 */
fun Row.projectLocationField(
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

fun Row.packageChooserField(
    project: Project,
    property: GraphProperty<String>,
): Cell<ExtendableTextField> {
    return cell(ExtendableTextField().apply {
        bind(property)
        addExtension(AllIcons.Nodes.Package) {
            val packageChooserDialog = PackageChooserDialog("Package Chooser", project)
            if (packageChooserDialog.showAndGet()) {
                packageChooserDialog.selectedPackage?.qualifiedName?.also {
                    property.set(it)
                }
            }
        }
    })
}

fun Row.relativeLocationField(
    project: Project,
    property: GraphProperty<String>,
): Cell<TextFieldWithBrowseButton> {
    val fileChooserDescriptor =
        FileChooserDescriptorFactory.createSingleFolderDescriptor()
            .withFileFilter { it.isDirectory }
            .withPathToTextConvertor {
                getPresentablePath(
                    Path(it).relativeTo(
                        project.guessProjectDir()?.toNioPath() ?: return@withPathToTextConvertor getPresentablePath(it)
                    ).pathString
                )
            }
            .withTextToPathConvertor {
                getCanonicalPath(
                    project.guessProjectDir()?.toNioPath()?.resolve(it)?.absolutePathString()
                        ?: return@withTextToPathConvertor getCanonicalPath(it)
                )
            }
    return textFieldWithBrowseButton("Select Source", project, fileChooserDescriptor).bindText(property)
}

fun Row.jarFileChooserField(
    locationProperty: GraphProperty<String>,
): Cell<TextFieldWithBrowseButton> {
    val fileChooserDescriptor =
        FileChooserDescriptorFactory.createSingleFileDescriptor("jar")
            .withFileFilter { it.isDirectory }
            .withPathToTextConvertor(::getPresentablePath)
            .withTextToPathConvertor(::getCanonicalPath)
    val property = locationProperty.transform(::getPresentablePath, ::getCanonicalPath)
    return textFieldWithBrowseButton("Jar Chooser", null, fileChooserDescriptor).bindText(property)
}