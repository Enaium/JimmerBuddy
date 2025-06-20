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
class DtoPropEditor(val node: DtoTree.DtoPropNode) : JPanel(BorderLayout()) {
    val project = node.target.project
    val model = PropModel()

    init {

        var nameEditor: DtoNameEditor? = null

        var modifierEditor: DtoModifierEditor? = null

        var aliasEditor: DtoAliasEditor? = null

        var aliasPatternEditor: DtoAliasPatternEditor? = null


        (node.target.positiveProp?.prop
            ?: node.target.negativeProp?.prop
            ?: node.target.userProp?.prop)?.also { prop ->

            nameEditor = DtoNameEditor(model.nameProperty, prop, false)

            modifierEditor = node.target.positiveProp?.let {
                DtoModifierEditor(
                    model.modifiersProperty,
                    { it.modifier?.let { modifier -> listOf(modifier) } ?: listOf() },
                    prop,
                    true
                )
            }

            aliasEditor = node.target.positiveProp?.let {
                DtoAliasEditor(
                    model.aliasProperty,
                    { it.alias },
                    prop
                )
            }
        }

        node.target.aliasGroup?.pattern?.also {
            aliasPatternEditor = DtoAliasPatternEditor(model.originProperty, model.replacementProperty, it)
        }

        add(panel {
            row {
                cell(JPanel(VerticalFlowLayout()).apply {
                    nameEditor?.also {
                        add(it)
                    }
                    modifierEditor?.also {
                        add(it)
                    }
                    aliasEditor?.also {
                        add(it)
                    }
                    aliasPatternEditor?.also {
                        add(it)
                    }
                }).align(Align.FILL)
            }
            row {
                button("Save") {
                    val document =
                        FileDocumentManager.getInstance().getDocument(node.target.containingFile.virtualFile)
                            ?: return@button

                    WriteCommandAction.writeCommandAction(project).run<Throwable> {
                        nameEditor?.save(document)
                        modifierEditor?.save(document)
                        aliasEditor?.save(document)
                        aliasPatternEditor?.save(document)
                        PsiDocumentManager.getInstance(project).commitDocument(document)
                    }
                }.align(Align.CENTER)
            }
        }, BorderLayout.CENTER)
    }

    inner class PropModel() : BaseState() {
        private val graph: PropertyGraph = PropertyGraph()
        val nameProperty = graph.property(
            node.target.positiveProp?.prop?.value
                ?: node.target.negativeProp?.prop?.value
                ?: node.target.userProp?.prop?.value
                ?: ""
        )
        val modifiersProperty =
            graph.property(node.target.positiveProp?.modifier?.value?.let { mutableSetOf(it) } ?: mutableSetOf())

        val aliasProperty = graph.property(node.target.positiveProp?.alias?.value ?: "")

        val originProperty = graph.property(
            node.target.aliasGroup?.pattern?.original?.value
                ?: if (node.target.aliasGroup?.pattern?.prefix == true) "^" else ""
        )
        val replacementProperty = graph.property(
            node.target.aliasGroup?.pattern?.replacement?.value
                ?: if (node.target.aliasGroup?.pattern?.suffix == true) "$" else ""
        )
    }
}