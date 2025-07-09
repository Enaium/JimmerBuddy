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
import cn.enaium.jimmer.buddy.utility.CommonImmutableType
import cn.enaium.jimmer.buddy.utility.I18n
import cn.enaium.jimmer.buddy.utility.findCurrentImmutableType
import cn.enaium.jimmer.buddy.utility.runReadActionSmart
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import javax.swing.JComboBox
import javax.swing.JComponent

/**
 * @author Enaium
 */
class AppendDtoProp(private val node: DtoTree.DtoNode) : DialogWrapper(false) {
    val project = node.target.project
    private val model = Model()

    init {
        title = I18n.message("dialog.appendDtoProp.title")
        setSize(300, 150)
        init()
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row(I18n.message("dialog.appendDtoProp.label.props")) {
                cell(JComboBox<CommonImmutableType.CommonImmutableProp>().apply {
                    CoroutineScope(Dispatchers.IO).launch {
                        node.target.project.runReadActionSmart {
                            findCurrentImmutableType(
                                if (node is DtoTree.DtoTypeNode) {
                                    node.target
                                } else {
                                    node.target.lastChild
                                }
                            )?.props()?.forEach {
                                addItem(it)
                            }
                        }
                    }
                    addActionListener {
                        (selectedItem as? CommonImmutableType.CommonImmutableProp)?.also {
                            this@AppendDtoProp.model.propProperty.set(it)
                        }
                    }
                }).align(Align.FILL)
            }
        }
    }

    override fun doOKAction() {
        model.prop?.also { prop ->
            val text = if (prop.isAssociation(true)) {
                "${prop.name()} {\n\t\n}"
            } else {
                prop.name()
            }

            WriteCommandAction.writeCommandAction(project).run<Throwable> {
                val document =
                    FileDocumentManager.getInstance().getDocument(node.target.containingFile.virtualFile) ?: return@run
                val offset = when (node) {
                    is DtoTree.DtoPropNode -> {
                        node.target.endOffset
                    }

                    is DtoTree.DtoTypeNode -> {
                        node.target.body?.explicitProps?.lastOrNull()?.endOffset
                            ?: node.target.body?.firstChild?.endOffset
                    }

                    else -> {
                        null
                    }
                }

                offset?.also {
                    document.insertString(offset, "\n\t$text")
                }
                PsiDocumentManager.getInstance(project).commitDocument(document)
                project.messageBus.syncPublisher(NEED_REFRESH_TOPIC).refresh()
            }
        }
        super.doOKAction()
    }

    private class Model : BaseState() {
        private val graph = PropertyGraph()
        val propProperty = graph.property<CommonImmutableType.CommonImmutableProp?>(null)
        val prop by propProperty
    }
}