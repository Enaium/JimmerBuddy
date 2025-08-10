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
import cn.enaium.jimmer.buddy.database.generate.JavaEntityGenerate
import cn.enaium.jimmer.buddy.database.generate.KotlinEntityGenerate
import cn.enaium.jimmer.buddy.database.model.GenerateEntityModel
import cn.enaium.jimmer.buddy.dialog.panel.TableTreeTable
import cn.enaium.jimmer.buddy.storage.DatabaseCache
import cn.enaium.jimmer.buddy.utility.I18n
import cn.enaium.jimmer.buddy.utility.getTables
import cn.enaium.jimmer.buddy.utility.packageChooserField
import cn.enaium.jimmer.buddy.utility.relativeLocationField
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.borderPanel
import javax.swing.JComponent

/**
 * @author Enaium
 */
class GenerateEntityDialog(
    private val project: Project,
    databaseItem: DatabaseCache.DatabaseItem
) : DialogWrapper(false) {

    private val generateEntityModel = GenerateEntityModel()
    private val tableTreeTable = TableTreeTable(
        try {
            databaseItem.getTables(project)
        } catch (e: Throwable) {
            Messages.showErrorDialog(
                I18n.message("dialog.generate.entity.message.connectFail", e.message),
                "Error"
            )
            JimmerBuddy.getWorkspace(project).log.error(e)
            emptySet()
        }
    )

    init {
        title = I18n.message("dialog.generate.entity.title")
        setSize(800, 600)
        init()
    }

    override fun createCenterPanel(): JComponent {
        return borderPanel {
            addToTop(panel {
                row(I18n.message("dialog.generate.entity.label.relativePath")) {
                    relativeLocationField(project, generateEntityModel.relativePathProperty).align(Align.FILL)
                }
                row(I18n.message("dialog.generate.entity.label.packageName")) {
                    packageChooserField(project, generateEntityModel.packageNameProperty).align(Align.FILL)
                }
                row(I18n.message("dialog.generate.entity.label.language")) {
                    JimmerBuddy.Services.UI.segmentedButtonText(this, GenerateEntityModel.Language.entries) {
                        it.text
                    }.bind(generateEntityModel.languageProperty)
                }

                collapsibleGroup(I18n.message("dialog.generate.entity.group.advanced")) {
                    row {
                        checkBox(I18n.message("dialog.generate.entity.checkbox.comment")).bindSelected(
                            generateEntityModel.commentProperty
                        )
                        checkBox(I18n.message("dialog.generate.entity.checkbox.table")).bindSelected(
                            generateEntityModel.tableAnnotationProperty
                        )
                        checkBox(I18n.message("dialog.generate.entity.checkbox.column")).bindSelected(
                            generateEntityModel.columnAnnotationProperty
                        )
                        checkBox(I18n.message("dialog.generate.entity.checkbox.idView")).bindSelected(
                            generateEntityModel.idViewAnnotationProperty
                        )
                        checkBox(I18n.message("dialog.generate.entity.checkbox.joinTable")).bindSelected(
                            generateEntityModel.joinTableAnnotationProperty
                        )
                    }
                    row(I18n.message("dialog.generate.entity.label.primaryKeyName")) {
                        textField().align(Align.FILL).bindText(generateEntityModel.primaryKeyNameProperty)
                    }
                    row(I18n.message("dialog.generate.entity.label.association")) {
                        JimmerBuddy.Services.UI.segmentedButtonText(this, GenerateEntityModel.Association.entries) {
                            it.text
                        }.bind(generateEntityModel.associationProperty)
                    }
                    row(I18n.message("dialog.generate.entity.label.tableNameRegex")) {
                        textField().align(Align.FILL).bindText(generateEntityModel.tableNameRegexProperty)
                        textField().align(Align.FILL).bindText(generateEntityModel.tableNameReplaceProperty)
                    }
                    row(I18n.message("dialog.generate.entity.label.columnNameRegex")) {
                        textField().align(Align.FILL).bindText(generateEntityModel.columnNameRegexProperty)
                        textField().align(Align.FILL).bindText(generateEntityModel.columnNameReplaceProperty)
                    }
                }
            })
            addToCenter(tableTreeTable)
        }
    }

    override fun doOKAction() {

        val result = tableTreeTable.getResult().takeIf { it.isNotEmpty() } ?: run {
            Messages.showErrorDialog(I18n.message("dialog.generate.entity.message.noSelectTable"), "Error")
            return
        }

        if (generateEntityModel.relativePath.isBlank()) {
            Messages.showErrorDialog(I18n.message("dialog.generate.entity.message.relativePathEmpty"), "Error")
            return
        }

        val generate = when (generateEntityModel.language) {
            GenerateEntityModel.Language.KOTLIN -> {
                KotlinEntityGenerate()
            }

            GenerateEntityModel.Language.JAVA -> {
                JavaEntityGenerate()
            }
        }

        JimmerBuddy.getWorkspace(project).asyncRefresh(
            generate.generate(
                project,
                generateEntityModel,
                result
            )
        )
        super.doOKAction()
    }

    enum class JdbcDriver(val className: String, val group: String, val artifact: String, val scheme: String) {
        POSTGRESQL("org.postgresql.Driver", "org.postgresql", "postgresql", "postgresql"),
        MARIADB("org.mariadb.jdbc.Driver", "org.mariadb.jdbc", "mariadb-java-client", "mariadb"),
        MYSQL("com.mysql.cj.jdbc.Driver", "com.mysql", "mysql-connector-j", "mysql"),
        SQLITE("org.sqlite.JDBC", "org.xerial", "sqlite-jdbc", "sqlite"),
        H2("org.h2.Driver", "com.h2database", "h2", "h2")
    }
}