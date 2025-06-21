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

import cn.enaium.jimmer.buddy.dialog.AppendDtoProp
import cn.enaium.jimmer.buddy.dialog.AppendDtoType
import cn.enaium.jimmer.buddy.extensions.dto.editor.notifier.NeedRefreshNotifier
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoType
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiElement
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiExplicitProp
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiRoot
import cn.enaium.jimmer.buddy.utility.expandAll
import cn.enaium.jimmer.buddy.utility.runReadActionSmart
import cn.enaium.jimmer.buddy.utility.runReadOnly
import com.intellij.icons.AllIcons
import cn.enaium.jimmer.buddy.utility.I18n
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.messages.Topic
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

/**
 * @author Enaium
 */
class DtoTree(val project: Project, private val file: VirtualFile) : JPanel() {
    private val root = DefaultMutableTreeNode()
    private val tree = Tree(root)

    private val connection = project.messageBus.connect()

    companion object {
        val NEED_REFRESH_TOPIC = Topic.create("Need Refresh", NeedRefreshNotifier::class.java)
    }

    init {
        connection.subscribe(NEED_REFRESH_TOPIC, object : NeedRefreshNotifier {
            override fun refresh() {
                this@DtoTree.refresh()
            }
        })

        layout = BorderLayout()
        tree.isRootVisible = false
        tree.cellRenderer = DtoNodeCell()
        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                tree.lastSelectedPathComponent?.also { select ->
                    if (select is DtoNode) {
                        project.messageBus.syncPublisher(DtoInfo.NODE_SELECTED_TOPIC).selectNode(select)
                    }

                    fun navigate() {
                        if (select is DtoNode) {
                            (select.target as Navigatable).navigate(true)
                        }
                    }

                    if (SwingUtilities.isRightMouseButton(e)) {
                        JBPopupMenu().apply {
                            add(JMenuItem(I18n.message("editor.dto.menu.goto")).apply {
                                addActionListener {
                                    navigate()
                                }
                            })
                            val appendDtoType =
                                JMenuItem(I18n.message("editor.dto.menu.appendDtoType")).apply {
                                    addActionListener {
                                        if (select is DtoTypeNode) {
                                            AppendDtoType(select).show()
                                        }
                                    }
                                }
                            val appendDtoProp =
                                JMenuItem(I18n.message("editor.dto.menu.appendDtoProp")).apply {
                                    addActionListener {
                                        AppendDtoProp(select as DtoNode).show()
                                    }
                                }

                            if (select is DtoTypeNode) {
                                add(appendDtoType)
                                add(appendDtoProp)
                            } else {
                                add(appendDtoProp)
                            }

                        }.show(tree, e.x, e.y)
                    }
                }
            }
        })
        add(JBScrollPane(tree), BorderLayout.CENTER)
        refresh()
        FileDocumentManager.getInstance().getDocument(file)?.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                refresh()
            }
        })
    }

    fun refresh() {
        val psiFile = file.toPsiFile(project) ?: return
        val psiRoot = psiFile.getChildOfType<DtoPsiRoot>() ?: return
        root.removeAllChildren()
        psiRoot.dtoTypes.forEach { dtoType ->
            val dtoTypeNode = DtoTypeNode(dtoType)

            fun addExplicitProp(explicitProp: DtoPsiExplicitProp, parent: DefaultMutableTreeNode) {
                val newChild = DtoPropNode(explicitProp)
                explicitProp.positiveProp?.body?.explicitProps?.forEach {
                    addExplicitProp(it, newChild)
                }
                parent.add(newChild)
            }

            dtoType.body?.explicitProps?.forEach {
                addExplicitProp(it, dtoTypeNode)
            }
            root.add(dtoTypeNode)
        }
        project.runReadActionSmart {
            (tree.model as DefaultTreeModel).nodeStructureChanged(root)
            tree.expandAll(TreePath(tree.model.root))
        }
    }

    private class DtoNodeCell() : DefaultTreeCellRenderer() {
        override fun getTreeCellRendererComponent(
            tree: JTree,
            value: Any,
            sel: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ): Component {

            if (value is DtoTypeNode) {
                icon = AllIcons.Nodes.Class
            } else if (value is DtoPropNode) {
                icon = AllIcons.Nodes.Property
            }

            return JPanel(BorderLayout()).apply {
                if (sel) {
                    setBackground(this@DtoNodeCell.getBackgroundSelectionColor())
                } else {
                    setBackground(this@DtoNodeCell.getBackground())
                }
                add(JLabel(runReadOnly { value.toString() }).apply {
                    icon = this@DtoNodeCell.icon
                }, BorderLayout.CENTER)
            }
        }
    }

    open class DtoNode(open val target: DtoPsiElement) : DefaultMutableTreeNode()

    class DtoTypeNode(override val target: DtoPsiDtoType) : DtoNode(target) {
        override fun toString(): String {
            return (target.name?.text ?: "Unknown Name").let {
                if (target.modifiers.isNotEmpty()) {
                    "$it ${target.modifiers.joinToString(" ", "(", ")") { it.value }} "
                } else {
                    it
                }
            }
        }
    }

    class DtoPropNode(override val target: DtoPsiExplicitProp) : DtoNode(target) {
        override fun toString(): String {
            return target.positiveProp?.prop?.text
                ?: target.negativeProp?.text
                ?: target.userProp?.text
                ?: target.aliasGroup?.pattern?.text
                ?: "Unknown Name"
        }
    }
}
