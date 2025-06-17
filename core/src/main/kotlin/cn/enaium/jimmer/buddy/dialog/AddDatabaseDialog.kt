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

import cn.enaium.jimmer.buddy.storage.JimmerBuddySetting
import cn.enaium.jimmer.buddy.storage.JimmerBuddySetting.DatabaseItem
import cn.enaium.jimmer.buddy.utility.fileChooserField
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/**
 * @author Enaium
 */
class AddDatabaseDialog(val select: DatabaseItem? = null) : DialogWrapper(false) {
    private val databaseModel = DatabaseModel()

    init {
        title = "Add Database"
        init()
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row("URI:") {
                fileChooserField(databaseModel.uriProperty, "sql", true).align(Align.FILL)
            }
            row("Username:") {
                textField().align(Align.FILL).bindText(databaseModel.usernameProperty)
            }
            row("Password:") {
                passwordField().align(Align.FILL).bindText(databaseModel.passwordProperty)
            }
            row("Catalog:") {
                textField().align(Align.FILL).bindText(databaseModel.catalogProperty)
            }
            row("Schema Pattern:") {
                textField().align(Align.FILL).bindText(databaseModel.schemaPatternProperty)
            }
            row("Table Name Pattern:") {
                textField().align(Align.FILL).bindText(databaseModel.tableNamePatternProperty)
            }
            collapsibleGroup("Driver") {
                row("Driver File:") {
                    fileChooserField(databaseModel.driverFileProperty, "jar").align(Align.FILL)
                }
                row("Driver Name:") {
                    textField().align(Align.FILL).bindText(databaseModel.driverNameProperty)
                }
            }
        }
    }

    override fun doOKAction() {
        if (databaseModel.uri.isBlank()) {
            Messages.showErrorDialog("URI cannot be empty", "Error")
            return
        }

        JimmerBuddySetting.INSTANCE.state.databases = JimmerBuddySetting.INSTANCE.state.databases + DatabaseItem(
            databaseModel.uri,
            databaseModel.username,
            databaseModel.password,
            databaseModel.catalog,
            databaseModel.schemaPattern,
            databaseModel.tableNamePattern,
            databaseModel.driverFile,
            databaseModel.driverName
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

        val uri: String by uriProperty
        val username: String by usernameProperty
        val password: String by passwordProperty
        val catalog: String by catalogProperty
        val schemaPattern: String by schemaPatternProperty
        val tableNamePattern: String by tableNamePatternProperty
        val driverFile: String by driverFileProperty
        val driverName: String by driverNameProperty
    }
}