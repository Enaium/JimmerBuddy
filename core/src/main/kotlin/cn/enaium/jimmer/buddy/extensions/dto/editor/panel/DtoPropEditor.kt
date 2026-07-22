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

import cn.enaium.jimmer.buddy.extensions.dto.editor.panel.DtoTree.Companion.findAliasElement
import cn.enaium.jimmer.buddy.extensions.dto.editor.panel.DtoTree.Companion.findModifierTokens
import cn.enaium.jimmer.buddy.extensions.dto.editor.panel.DtoTree.Companion.findPropIdentifier
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoTypes
import cn.enaium.jimmer.buddy.utility.I18n
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.elementType
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


        (node.target.positiveProp?.let { findPropIdentifier(it) }
            ?: node.target.negativeProp?.propName?.identifier
            ?: node.target.userProp?.identifier)?.also { prop ->

            nameEditor = DtoNameEditor(model.nameProperty, prop, false)

            modifierEditor = node.target.positiveProp?.let {
                DtoModifierEditor(
                    model.modifiersProperty,
                    { findModifierTokens(it) },
                    prop,
                    true
                )
            }

            aliasEditor = node.target.positiveProp?.let {
                DtoAliasEditor(
                    model.aliasProperty,
                    { findAliasElement(it) },
                    prop
                )
            }
        }

        node.target.aliasGroup?.aliasPattern?.also {
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
                button(I18n.message("editor.dto.button.save")) {
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

        private fun getAliasText(): String {
            val positiveProp = node.target.positiveProp ?: return ""
            val aliasIdentifier = positiveProp.alias?.identifier?.text ?: return ""
            return "as $aliasIdentifier"
        }

        val nameProperty = graph.property(
            node.target.positiveProp?.let { findPropIdentifier(it) }?.text
                ?: node.target.negativeProp?.propName?.identifier?.text
                ?: node.target.userProp?.identifier?.text
                ?: ""
        )
        val modifiersProperty =
            graph.property(
                node.target.positiveProp?.let { findModifierTokens(it).map { it.text }.toMutableSet() }
                    ?: mutableSetOf()
            )

        val aliasProperty = graph.property(getAliasText())

        private fun getOriginText(): String {
            val aliasPattern = node.target.aliasGroup?.aliasPattern ?: return ""
            val children = aliasPattern.children
            val caret = children.find { it.elementType == DtoTypes.CARET }
            val originIdentifier = children.find {
                it.elementType == DtoTypes.IDENTIFIER && caret?.textRange?.endOffset?.let { end ->
                    it.textRange.startOffset < end
                } != true && it.textRange.startOffset < (children.find { it.elementType == DtoTypes.ARROW }
                    ?.textRange?.startOffset ?: Int.MAX_VALUE)
            }
            return if (caret != null) "^" else (originIdentifier?.text ?: "")
        }

        private fun getReplacementText(): String {
            val aliasPattern = node.target.aliasGroup?.aliasPattern ?: return ""
            val children = aliasPattern.children
            val dollar = children.find { it.elementType == DtoTypes.DOLLAR }
            val arrow = children.find { it.elementType == DtoTypes.ARROW }
            val replacementIdentifier = children.find {
                it.elementType == DtoTypes.IDENTIFIER && (arrow?.textRange?.startOffset
                    ?: 0) < it.textRange.startOffset
            }
            return if (dollar != null) "$" else (replacementIdentifier?.text ?: "")
        }

        val originProperty = graph.property(getOriginText())
        val replacementProperty = graph.property(getReplacementText())
    }
}