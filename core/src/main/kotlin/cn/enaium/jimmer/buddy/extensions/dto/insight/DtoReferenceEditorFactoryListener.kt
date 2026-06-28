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

package cn.enaium.jimmer.buddy.extensions.dto.insight

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoType
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiFile
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiName
import cn.enaium.jimmer.buddy.utility.generatedReferences
import cn.enaium.jimmer.buddy.utility.runReadOnly
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseEventArea
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import java.util.concurrent.ConcurrentHashMap
import javax.swing.SwingUtilities

class DtoReferenceEditorFactoryListener : EditorFactoryListener {
    private val listeners = ConcurrentHashMap<Editor, EditorMouseListener>()

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val listener = DtoReferenceMouseListener()
        listeners[editor] = listener
        editor.addEditorMouseListener(listener)
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        val editor = event.editor
        val listener = listeners.remove(editor) ?: return
        editor.removeEditorMouseListener(listener)
    }

    private class DtoReferenceMouseListener : EditorMouseListener {
        override fun mousePressed(event: EditorMouseEvent) {
            if (!event.shouldHandleDtoReferenceClick()) {
                return
            }

            val references = event.findDtoReferencesAtMouseOffset()
            if (references.isEmpty()) {
                return
            }

            if (DtoReferencePreview.show(references, event.mouseEvent)) {
                event.consume()
                event.mouseEvent.consume()
            }
        }

        private fun EditorMouseEvent.shouldHandleDtoReferenceClick(): Boolean {
            val mouseEvent = mouseEvent
            if (!SwingUtilities.isLeftMouseButton(mouseEvent)) {
                return false
            }

            if (mouseEvent.clickCount != 1) {
                return false
            }

            if (area != EditorMouseEventArea.EDITING_AREA || !isOverText) {
                return false
            }

            val clickEditor = editor
            if (clickEditor.isViewer || clickEditor.selectionModel.hasSelection()) {
                return false
            }

            return true
        }

        private fun EditorMouseEvent.findDtoReferencesAtMouseOffset(): List<PsiElement> {
            val project = editor.project ?: return emptyList()
            val documentManager = PsiDocumentManager.getInstance(project)
            documentManager.commitDocument(editor.document)

            return runReadOnly {
                val file = documentManager.getPsiFile(editor.document) as? DtoPsiFile ?: return@runReadOnly emptyList()
                val dtoType = findDtoTypeAtOffset(file, offset) ?: return@runReadOnly emptyList()
                dtoType.generatedReferences()
            }
        }

        private fun findDtoTypeAtOffset(
            file: DtoPsiFile,
            offset: Int,
        ): DtoPsiDtoType? {
            val safeOffset = offset.coerceIn(0, file.textLength)
            val previousOffset = (safeOffset - 1).coerceAtLeast(0)
            val name = listOf(safeOffset, previousOffset)
                .asSequence()
                .mapNotNull { file.findElementAt(it) }
                .mapNotNull { PsiTreeUtil.getParentOfType(it, DtoPsiName::class.java, false) }
                .firstOrNull { it.textRange.containsOffset(safeOffset) || it.textRange.containsOffset(previousOffset) }
                ?: return null
            val dtoType = name.parent as? DtoPsiDtoType ?: return null
            if (dtoType.name != name) {
                return null
            }

            return dtoType
        }
    }
}
