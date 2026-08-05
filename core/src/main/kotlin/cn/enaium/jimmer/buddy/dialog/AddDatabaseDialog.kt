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

import cn.enaium.jimmer.buddy.database.provider.IntelliJDatabaseMetadataProvider
import cn.enaium.jimmer.buddy.database.provider.IntelliJDataSourceInfo
import cn.enaium.jimmer.buddy.storage.DatabaseCache
import cn.enaium.jimmer.buddy.storage.DatabaseCache.DatabaseItem
import cn.enaium.jimmer.buddy.utility.I18n
import cn.enaium.jimmer.buddy.utility.fileChooserField
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent

/**
 * @author Enaium
 */
class AddDatabaseDialog(val project: Project, val select: DatabaseItem? = null) : DialogWrapper(false) {
    private val databaseModel = DatabaseModel()

    private val intelliJDataSources: List<IntelliJDataSourceInfo> by lazy {
        if (IntelliJDatabaseMetadataProvider.isDatabasePluginAvailable()) {
            IntelliJDatabaseMetadataProvider().getDataSources(project)
        } else {
            emptyList()
        }
    }

    init {
        title = I18n.message("dialog.addDatabase.title")
        init()
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            if (intelliJDataSources.isNotEmpty()) {
                row("IntelliJ Data Source") {
                    comboBox(
                        DefaultComboBoxModel(
                            arrayOf("") + intelliJDataSources.map { "${it.name} (${it.url ?: "No URL"})" }.toTypedArray()
                        )
                    ).align(Align.FILL).apply {
                        component.selectedItem = select?.intellijDataSourceId?.let { id ->
                            intelliJDataSources.find { it.id == id }?.let { ds ->
                                "${ds.name} (${ds.url ?: "No URL"})"
                            }
                        } ?: ""
                        component.addActionListener {
                            val selected = component.selectedItem as? String
                            if (selected != null && selected.isNotEmpty()) {
                                val index = component.selectedIndex - 1
                                if (index >= 0 && index < intelliJDataSources.size) {
                                    val ds = intelliJDataSources[index]
                                    databaseModel.uriProperty.set(ds.url ?: "")
                                    databaseModel.usernameProperty.set(ds.username ?: "")
                                    databaseModel.driverNameProperty.set(ds.driverClass ?: "")
                                    databaseModel.intellijDataSourceIdProperty.set(ds.id)
                                }
                            } else {
                                databaseModel.intellijDataSourceIdProperty.set("")
                            }
                        }
                    }
                }
            }
            row(I18n.message("dialog.addDatabase.label.uri")) {
                fileChooserField(databaseModel.uriProperty, "sql", true).align(Align.FILL)
            }
            row(I18n.message("dialog.addDatabase.label.username")) {
                textField().align(Align.FILL).bindText(databaseModel.usernameProperty)
            }
            row(I18n.message("dialog.addDatabase.label.password")) {
                passwordField().align(Align.FILL).bindText(databaseModel.passwordProperty)
            }
            row(I18n.message("dialog.addDatabase.label.catalog")) {
                textField().align(Align.FILL).bindText(databaseModel.catalogProperty)
            }
            row(I18n.message("dialog.addDatabase.label.schemaPattern")) {
                textField().align(Align.FILL).bindText(databaseModel.schemaPatternProperty)
            }
            row(I18n.message("dialog.addDatabase.label.tableNamePattern")) {
                textField().align(Align.FILL).bindText(databaseModel.tableNamePatternProperty)
            }
            collapsibleGroup(I18n.message("dialog.addDatabase.group.driver")) {
                row(I18n.message("dialog.addDatabase.label.driverFile")) {
                    fileChooserField(databaseModel.driverFileProperty, "jar").align(Align.FILL)
                }
                row(I18n.message("dialog.addDatabase.label.driverName")) {
                    textField().align(Align.FILL).bindText(databaseModel.driverNameProperty)
                }
            }
        }
    }

    override fun doOKAction() {
        if (databaseModel.uri.isBlank()) {
            Messages.showErrorDialog(I18n.message("dialog.addDatabase.message.uriEmpty"), "Error")
            return
        }

        val databaseCache = DatabaseCache.getInstance(project)
        databaseCache.databases += DatabaseItem(
            databaseModel.uri,
            databaseModel.username,
            databaseModel.password,
            databaseModel.catalog,
            databaseModel.schemaPattern,
            databaseModel.tableNamePattern,
            databaseModel.driverFile,
            databaseModel.driverName,
            databaseModel.intellijDataSourceId
        )
        super.doOKAction()
    }

    private inner class DatabaseModel : BaseState() {
        private val graph: PropertyGraph = PropertyGraph()
        val uriProperty = graph.property(select?.uri ?: "")
        val usernameProperty = graph.property(select?.username ?: "")
        val passwordProperty = graph.property(select?.password ?: "")
        val catalogProperty = graph.property(select?.catalog ?: "")
        val schemaPatternProperty = graph.property(select?.schemaPattern ?: "")
        val tableNamePatternProperty = graph.property(select?.tableNamePattern ?: "")
        val driverFileProperty = graph.property(select?.driverFile ?: "")
        val driverNameProperty = graph.property(select?.driverName ?: "")
        val intellijDataSourceIdProperty = graph.property(select?.intellijDataSourceId ?: "")

        val uri: String by uriProperty
        val username: String by usernameProperty
        val password: String by passwordProperty
        val catalog: String by catalogProperty
        val schemaPattern: String by schemaPatternProperty
        val tableNamePattern: String by tableNamePatternProperty
        val driverFile: String by driverFileProperty
        val driverName: String by driverNameProperty
        val intellijDataSourceId: String by intellijDataSourceIdProperty
    }
}