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

import cn.enaium.jimmer.buddy.utility.runReadOnly
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListCellRenderer
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.ListSelectionModel

internal object DtoReferencePreview {
    fun show(
        references: List<PsiElement>,
        event: MouseEvent,
    ): Boolean {
        val validReferences = references.filter { it.isValid }
        if (validReferences.isEmpty()) {
            JBPopupFactory.getInstance()
                .createMessage("No DTO references found")
                .show(RelativePoint(event))
            return true
        }

        DtoReferencePreviewPopup(validReferences).show(event)
        return true
    }
}

private class DtoReferencePreviewPopup(
    private val references: List<PsiElement>,
) {
    private val project = references.first().project
    private val referenceList = JBList(references)
    private val previewPanel = JPanel(BorderLayout())
    private var editor: Editor? = null
    private var popup: JBPopup? = null

    fun show(event: MouseEvent) {
        referenceList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        referenceList.cellRenderer = DtoReferenceListRenderer()
        referenceList.selectedIndex = 0
        referenceList.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) {
                updatePreview(referenceList.selectedValue)
            }
        }
        referenceList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (event.clickCount == 2) {
                    navigateSelectedReference()
                }
            }
        })
        referenceList.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(event: KeyEvent) {
                if (event.keyCode == KeyEvent.VK_ENTER) {
                    navigateSelectedReference()
                }
            }
        })

        updatePreview(referenceList.selectedValue)

        val content = createContent()
        val createdPopup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(content, referenceList)
            .setTitle("Choose DTO Reference")
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .setProject(project)
            .createPopup()

        popup = createdPopup
        Disposer.register(createdPopup, Disposable { releaseEditor() })
        createdPopup.show(RelativePoint(event))
    }

    private fun createContent(): JComponent {
        val listPanel = JBScrollPane(referenceList)
        listPanel.preferredSize = Dimension(360, 560)
        previewPanel.preferredSize = Dimension(760, 560)

        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, previewPanel)
        splitPane.preferredSize = Dimension(1120, 560)
        splitPane.dividerLocation = 360
        splitPane.resizeWeight = 0.0
        return splitPane
    }

    private fun updatePreview(reference: PsiElement?) {
        previewPanel.removeAll()
        releaseEditor()

        if (reference == null || !reference.isValid) {
            previewPanel.add(JLabel("No preview available"), BorderLayout.CENTER)
            refreshPreviewPanel()
            return
        }

        val preview = createPreview(reference)
        if (preview == null) {
            previewPanel.add(JLabel("No preview available"), BorderLayout.CENTER)
            refreshPreviewPanel()
            return
        }

        val createdEditor = createPreviewEditor(preview)
        editor = createdEditor
        previewPanel.add(createdEditor.component, BorderLayout.CENTER)
        createdEditor.caretModel.moveToOffset(preview.offset)
        createdEditor.selectionModel.setSelection(preview.offset, preview.endOffset)
        createdEditor.scrollingModel.scrollToCaret(ScrollType.CENTER)
        refreshPreviewPanel()
    }

    private fun createPreview(reference: PsiElement): DtoReferencePreviewInfo? {
        return runReadOnly {
            if (!reference.isValid) {
                return@runReadOnly null
            }

            val file = reference.containingFile ?: return@runReadOnly null
            val document = PsiDocumentManager.getInstance(project).getDocument(file)
                ?: EditorFactory.getInstance().createDocument(file.text)
            val offset = reference.textRange.startOffset.coerceIn(0, document.textLength)
            val endOffset = reference.textRange.endOffset.coerceIn(offset, document.textLength)

            DtoReferencePreviewInfo(
                document = document,
                virtualFile = file.virtualFile,
                offset = offset,
                endOffset = endOffset,
            )
        }
    }

    private fun createPreviewEditor(preview: DtoReferencePreviewInfo): Editor {
        val factory = EditorFactory.getInstance()
        val virtualFile = preview.virtualFile
        if (virtualFile != null) {
            return factory.createEditor(preview.document, project, virtualFile, true)
        }

        return factory.createViewer(preview.document, project)
    }

    private fun navigateSelectedReference() {
        val reference = referenceList.selectedValue ?: return
        val location = reference.navigationLocation() ?: return
        popup?.cancel()
        OpenFileDescriptor(project, location.virtualFile, location.offset).navigate(true)
    }

    private fun PsiElement.navigationLocation(): DtoReferenceNavigationLocation? {
        return runReadOnly {
            if (!isValid) {
                return@runReadOnly null
            }

            val file = containingFile?.virtualFile ?: return@runReadOnly null
            val offset = textRange.startOffset
            DtoReferenceNavigationLocation(file, offset)
        }
    }

    private fun refreshPreviewPanel() {
        previewPanel.revalidate()
        previewPanel.repaint()
    }

    private fun releaseEditor() {
        val oldEditor = editor ?: return
        editor = null
        if (!oldEditor.isDisposed) {
            EditorFactory.getInstance().releaseEditor(oldEditor)
        }
    }
}

private class DtoReferenceListRenderer : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: JList<*>,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): Component {
        val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        val label = component as JLabel
        val element = value as? PsiElement ?: return label
        val presentation = runReadOnly {
            element.referencePresentation()
        }

        label.text = "${presentation.locationText}  ${presentation.lineText}"
        label.toolTipText = presentation.lineText
        label.icon = element.getIcon(0)
        return label
    }
}

private data class DtoReferencePreviewInfo(
    val document: Document,
    val virtualFile: VirtualFile?,
    val offset: Int,
    val endOffset: Int,
)

private data class DtoReferenceNavigationLocation(
    val virtualFile: VirtualFile,
    val offset: Int,
)

private data class DtoReferencePresentation(
    val lineText: String,
    val locationText: String,
)

private fun PsiElement.referencePresentation(): DtoReferencePresentation {
    val file = containingFile ?: return DtoReferencePresentation(text, "")
    val fileName = file.virtualFile?.name ?: file.name
    val document = PsiDocumentManager.getInstance(project).getDocument(file)

    if (document == null) {
        return DtoReferencePresentation(text.normalizeCodeLine(), fileName)
    }

    val line = document.getLineNumber(textRange.startOffset)
    val lineStart = document.getLineStartOffset(line)
    val lineEnd = document.getLineEndOffset(line)
    val lineText = document.text.substring(lineStart, lineEnd).normalizeCodeLine()

    return DtoReferencePresentation(
        lineText.ifEmpty { text.normalizeCodeLine() },
        "$fileName:${line + 1}",
    )
}

private fun String.normalizeCodeLine(): String {
    return trim().replace(Regex("\\s+"), " ")
}
