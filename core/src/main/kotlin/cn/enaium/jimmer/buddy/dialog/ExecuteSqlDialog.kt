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

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBLabel
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.borderPanel
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.JComponent


/**
 * @author Enaium
 */
class ExecuteSqlDialog(val project: Project, val sql: String) : DialogWrapper(false) {
    init {
        title = "Execute SQL"
        setSize(800, 600)
        init()
    }

    override fun createCenterPanel(): JComponent {
        return borderPanel {
            addToTop(JBLabel("Recommend to enable Inline SQL Parameters in Jimmer"))
            addToCenter(
                object : EditorTextField(
                    EditorFactory.getInstance().createDocument(sql),
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
                })
            addToBottom(JBLabel("Ok is copy to clipboard, Cancel is close"))
        }
    }

    override fun doOKAction() {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(sql), null)
        super.doOKAction()
    }
}