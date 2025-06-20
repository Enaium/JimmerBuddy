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

package cn.enaium.jimmer.buddy.extensions.dto.editor.panel

import cn.enaium.jimmer.buddy.extensions.dto.editor.panel.DtoTree.Companion.NEED_REFRESH_TOPIC
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * @author Enaium
 */
class DtoTypeEditor(val node: DtoTree.DtoTypeNode) : JPanel(BorderLayout()) {
    val project = node.target.project
    val model = TypeModel()

    init {
        node.target.name?.also { name ->

            val nameEditor = DtoNameEditor(model.nameProperty, name)

            val modifierEditor = DtoModifierEditor(
                model.modifiersProperty,
                { node.target.modifiers },
                name
            )

            add(panel {
                row {
                    cell(JPanel(VerticalFlowLayout()).apply {
                        add(nameEditor)
                        add(modifierEditor)
                    }).align(Align.FILL)
                }
                row {
                    button("Save") {
                        val document =
                            FileDocumentManager.getInstance().getDocument(node.target.containingFile.virtualFile)
                                ?: return@button

                        WriteCommandAction.writeCommandAction(project).run<Throwable> {
                            nameEditor.save(document)
                            modifierEditor.save(document)
                            PsiDocumentManager.getInstance(project).commitDocument(document)
                            project.messageBus.syncPublisher(NEED_REFRESH_TOPIC).refresh()
                        }
                    }.align(Align.CENTER)
                }
            }, BorderLayout.CENTER)
        }
    }

    inner class TypeModel : BaseState() {
        private val graph: PropertyGraph = PropertyGraph()
        val nameProperty = graph.property(node.target.name?.value ?: "")
        val modifiersProperty = graph.property(node.target.modifiers.map { it.value }.toMutableSet())
    }
}