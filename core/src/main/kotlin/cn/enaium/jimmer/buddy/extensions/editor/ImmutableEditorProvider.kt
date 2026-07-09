package cn.enaium.jimmer.buddy.extensions.editor

import cn.enaium.jimmer.buddy.utility.hasImmutableAnnotation
import cn.enaium.jimmer.buddy.utility.hasImmutableAnnotationBySyntax
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

/**
 * @author Enaium
 */
class ImmutableEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(
        project: Project,
        file: VirtualFile
    ): Boolean {
        return when (file.fileType) {
            is JavaFileType -> {
                file.toPsiFile(project)?.getChildOfType<PsiClass>()?.hasImmutableAnnotation() == true
            }

            is KotlinFileType -> {
                file.toPsiFile(project)?.getChildOfType<KtClass>()?.hasImmutableAnnotationBySyntax() == true
            }

            else -> false
        }
    }

    override fun createEditor(
        project: Project,
        file: VirtualFile
    ): FileEditor {
        return ImmutableViewerSplitEditor(
            TextEditorProvider.getInstance().createEditor(project, file) as TextEditor,
            ImmutableVisualFileEditor(project, file)
        )
    }

    override fun getEditorTypeId(): String {
        return "JimmerBuddy.Entity.Visual.Editor"
    }

    override fun getPolicy(): FileEditorPolicy {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR
    }
}