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
import cn.enaium.jimmer.buddy.database.generate.*
import cn.enaium.jimmer.buddy.database.model.GenerateDDLModel
import cn.enaium.jimmer.buddy.utility.CommonImmutableType
import cn.enaium.jimmer.buddy.utility.I18n
import cn.enaium.jimmer.buddy.utility.runReadOnly
import cn.enaium.jimmer.buddy.utility.thread
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.borderPanel
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.JComponent

/**
 * @author Enaium
 */
class GenerateDDLDialog(val project: Project, val commonImmutables: Set<CommonImmutableType>) : DialogWrapper(false) {
    private val generateDDLModel = GenerateDDLModel()
    private val editor =
        object : EditorTextField(
            EditorFactory.getInstance().createDocument(""),
            project,
            PlainTextFileType.INSTANCE,
            false
        ) {
            override fun createEditor(): EditorEx {
                return super.createEditor().apply {
                    isOneLineMode = false
                    setVerticalScrollbarVisible(true)
                    setHorizontalScrollbarVisible(true)
                    settings.isLineNumbersShown = true
                }
            }
        }

    init {
        title = I18n.message("dialog.generate.ddl.title")
        setSize(800, 600)
        init()
        generate()
    }

    override fun createCenterPanel(): JComponent {
        return borderPanel {
            addToTop(panel {
                row(I18n.message("dialog.generate.ddl.label.database")) {
                    JimmerBuddy.Services.UI.segmentedButtonText(this, DDLGenerate.Database.entries) {
                        it.text
                    }.bind(generateDDLModel.databaseProperty)
                }
                row {
                    checkBox(I18n.message("dialog.generate.ddl.checkbox.reference")).bindSelected(generateDDLModel.referenceProperty)
                    checkBox(I18n.message("dialog.generate.ddl.checkbox.comment")).bindSelected(generateDDLModel.commentProperty)
                    checkBox(I18n.message("dialog.generate.ddl.checkbox.ifNotExists")).bindSelected(generateDDLModel.ifNotExistsProperty)
                }
                row(I18n.message("dialog.generate.ddl.label.primaryKeyName")) {
                    textField().align(Align.FILL).bindText(generateDDLModel.primaryKeyNameProperty)
                }
                row {
                    button(I18n.message("dialog.generate.ddl.button.generate")) {
                        generate()
                    }
                }
            })
            addToCenter(editor)
            addToBottom(JBLabel(I18n.message("dialog.generate.ddl.copy")))
        }
    }

    private fun generate() {
        val generateDDL = when (generateDDLModel.database) {
            DDLGenerate.Database.POSTGRES -> {
                PostgresDDLGenerate(project, generateDDLModel)
            }

            DDLGenerate.Database.MYSQL -> {
                MySqlDDLGenerate(project, generateDDLModel)
            }

            DDLGenerate.Database.MARIADB -> {
                MariadbGenerate(project, generateDDLModel)
            }

            DDLGenerate.Database.SQLITE -> {
                SQLiteDDLGenerate(project, generateDDLModel)
            }

            DDLGenerate.Database.H2 -> {
                H2DDLGenerate(project, generateDDLModel)
            }

            DDLGenerate.Database.ORACLE -> {
                OracleDDLGenerate(project, generateDDLModel)
            }

        }

        editor.text = thread {
            runReadOnly {
                var lines = commonImmutables.joinToString("\n\n") { generateDDL.generate(it) }.split("\n")
                val alterLines = mutableListOf<String>()
                lines = lines.filter {
                    val alter = it.startsWith("alter")
                    if (alter) {
                        alterLines.add(it)
                    }
                    !alter
                }
                alterLines.sortBy { !it.contains("primary key") }
                (lines + alterLines).joinToString("\n")
            }
        }
    }

    override fun doOKAction() {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(editor.text), null)
        super.doOKAction()
    }
}