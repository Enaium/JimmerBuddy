package cn.enaium.jimmer.buddy.extensions.editor

import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview

/**
 * @author Enaium
 */
class ImmutableViewerSplitEditor(editor: TextEditor, visual: ImmutableVisualFileEditor) :
    TextEditorWithPreview(editor, visual, "", Layout.SHOW_EDITOR)
