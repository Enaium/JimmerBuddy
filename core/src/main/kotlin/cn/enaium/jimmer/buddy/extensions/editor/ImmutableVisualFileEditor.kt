package cn.enaium.jimmer.buddy.extensions.editor

import cn.enaium.jimmer.buddy.extensions.editor.panel.ImmutableDesigner
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import java.beans.PropertyChangeListener
import javax.swing.JComponent

/**
 * @author Enaium
 */
class ImmutableVisualFileEditor(project: Project, private val file: VirtualFile) : UserDataHolderBase(), FileEditor {

    val panel = ImmutableDesigner(project, file)

    override fun getComponent(): JComponent {
        return panel
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return null
    }

    override fun getName(): String {
        return "Entity Designer"
    }

    override fun setState(p0: FileEditorState) {
    }

    override fun isModified(): Boolean {
        return false
    }

    override fun isValid(): Boolean {
        return panel.isValid
    }

    override fun addPropertyChangeListener(p0: PropertyChangeListener) {
    }

    override fun removePropertyChangeListener(p0: PropertyChangeListener) {
    }

    override fun dispose() {
    }

    override fun getFile(): VirtualFile {
        return file
    }
}