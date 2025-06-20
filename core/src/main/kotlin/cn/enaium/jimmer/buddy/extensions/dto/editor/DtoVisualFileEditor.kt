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

package cn.enaium.jimmer.buddy.extensions.dto.editor

import cn.enaium.jimmer.buddy.extensions.dto.editor.panel.DtoInfo
import cn.enaium.jimmer.buddy.extensions.dto.editor.panel.DtoTree
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBSplitter
import com.intellij.ui.OnePixelSplitter
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.borderPanel
import java.beans.PropertyChangeListener
import javax.swing.JComponent

/**
 * @author Enaium
 */
class DtoVisualFileEditor(private val project: Project, private val file: VirtualFile) : UserDataHolderBase(),
    FileEditor {
    override fun getComponent(): JComponent {
        return borderPanel {
            addToCenter(
                OnePixelSplitter(true, 0.5f).apply {
                    firstComponent = DtoTree(project, file)
                    secondComponent = DtoInfo(project)
                }
            )
        }
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return null
    }

    override fun getName(): String {
        return "DTO Designer"
    }

    override fun setState(p0: FileEditorState) {}

    override fun isModified(): Boolean {
        return false
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun addPropertyChangeListener(p0: PropertyChangeListener) {}

    override fun removePropertyChangeListener(p0: PropertyChangeListener) {}

    override fun dispose() {

    }

    override fun getFile(): VirtualFile {
        return file
    }
}