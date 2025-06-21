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
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiElement
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiModifier
import cn.enaium.jimmer.buddy.utility.I18n
import com.intellij.openapi.editor.Document
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import org.babyfish.jimmer.dto.compiler.DtoModifier
import java.awt.FlowLayout
import javax.swing.JPanel

/**
 * @author Enaium
 */
class DtoModifierEditor(
    private val modifiersProperty: ObservableMutableProperty<MutableSet<String>>,
    private val modifiers: () -> List<DtoPsiModifier>,
    private val psiName: DtoPsiElement,
    private val single: Boolean = false
) : DtoBaseEditor() {

    val project = psiName.project

    init {
        init()
    }

    override fun panel(): JPanel {
        return panel {
            row(I18n.message("editor.dto.label.modifiers")) {}
            row {
                cell(JBScrollPane(JPanel(FlowLayout(FlowLayout.LEFT)).apply flow@{
                    DtoModifier.entries.map { it.name.lowercase() }.forEach { modifier ->
                        add(JBCheckBox(modifier).apply {
                            isSelected = modifier in modifiers().map { it.value }
                            if (isSelected) {
                                modifiersProperty.get().add(modifier)
                            }
                            addActionListener {
                                if (single) {
                                    modifiersProperty.get().clear()
                                    this@flow.components.forEach {
                                        if (it != this) {
                                            (it as? JBCheckBox)?.isSelected = false
                                        }
                                    }
                                }
                                if (isSelected) {
                                    modifiersProperty.get().add(modifier)
                                } else {
                                    modifiersProperty.get().remove(modifier)
                                }
                            }
                        })
                    }
                }).apply {
                    verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_NEVER
                }).align(Align.FILL)
            }
        }
    }

    override fun save(document: Document) {
        val get = modifiersProperty.get()
        if (modifiers().isNotEmpty()) {
            if (get.isEmpty()) {
                document.replaceString(
                    modifiers().first().startOffset,
                    psiName.startOffset,
                    ""
                )
            } else {
                document.replaceString(
                    modifiers().first().startOffset,
                    modifiers().last().endOffset,
                    get.joinToString(" ")
                )
            }
        } else {
            if (get.isNotEmpty()) {
                document.insertString(
                    psiName.startOffset, "${get.joinToString(" ")} "
                )
            }
        }
        project.messageBus.syncPublisher(NEED_REFRESH_TOPIC).refresh()
    }
}