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
class GenerateDDLDialog(val project: Project, val commonImmutable: CommonImmutableType) : DialogWrapper(false) {
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
        title = "Generate DDL"
        setSize(800, 600)
        init()
        generate()
    }

    override fun createCenterPanel(): JComponent {
        return borderPanel {
            addToTop(panel {
                row("Database:") {
                    JimmerBuddy.Services.UI.segmentedButtonText(this, DDLGenerate.Database.entries) {
                        it.text
                    }.bind(generateDDLModel.databaseProperty)
                }
                row {
                    checkBox("Reference").bindSelected(generateDDLModel.referenceProperty)
                }
                row("Primary Key Name:") {
                    textField().align(Align.FILL).bindText(generateDDLModel.primaryKeyNameProperty)
                }
                row {
                    button("Generate") {
                        generate()
                    }
                }
            })
            addToCenter(editor)
            addToBottom(JBLabel("Ok is copy to clipboard, Cancel is close"))
        }
    }

    private fun generate() {
        val generateDDL = when (generateDDLModel.database) {
            DDLGenerate.Database.POSTGRES -> {
                PostgresDDLGenerate(generateDDLModel)
            }

            DDLGenerate.Database.MYSQL -> {
                MySqlDDLGenerate(generateDDLModel)
            }

            DDLGenerate.Database.MARIADB -> {
                MariadbGenerate(generateDDLModel)
            }

            DDLGenerate.Database.SQLITE -> {
                SQLiteDDLGenerate(generateDDLModel)
            }

            DDLGenerate.Database.H2 -> {
                H2DDLGenerate(generateDDLModel)
            }

            DDLGenerate.Database.ORACLE -> {
                OracleDDLGenerate(generateDDLModel)
            }

        }

        editor.text = thread { runReadOnly { generateDDL.generate(commonImmutable) } }
    }

    override fun doOKAction() {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(editor.text), null)
        super.doOKAction()
    }
}