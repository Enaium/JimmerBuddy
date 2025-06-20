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

import cn.enaium.jimmer.buddy.extensions.dto.editor.panel.DtoTree
import cn.enaium.jimmer.buddy.extensions.dto.editor.panel.DtoTree.Companion.NEED_REFRESH_TOPIC
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import javax.swing.JComponent

/**
 * @author Enaium
 */
class AppendDtoType(private val node: DtoTree.DtoTypeNode) : DialogWrapper(false) {
    val project = node.target.project
    private val model = Model()

    init {
        title = "Append DTO Type"
        setSize(300, 150)
        init()
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row("Name:") {
                textField().align(Align.FILL).bindText(model.nameProperty)
            }
        }
    }

    private class Model : BaseState() {
        private val graph: PropertyGraph = PropertyGraph()
        val nameProperty = graph.property("")
        val name by nameProperty
    }


    override fun doOKAction() {
        WriteCommandAction.writeCommandAction(project).run<Throwable> {
            val document =
                FileDocumentManager.getInstance().getDocument(node.target.containingFile.virtualFile) ?: return@run
            document.insertString(node.target.endOffset, "\n\n${model.name} {\n\n}")
            PsiDocumentManager.getInstance(project).commitDocument(document)
            project.messageBus.syncPublisher(NEED_REFRESH_TOPIC).refresh()
        }
        super.doOKAction()
    }
}