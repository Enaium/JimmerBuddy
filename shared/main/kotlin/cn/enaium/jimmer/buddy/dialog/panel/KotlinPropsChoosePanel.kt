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

package cn.enaium.jimmer.buddy.dialog.panel

import com.intellij.icons.AllIcons
import com.intellij.ui.treeStructure.Tree
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableProp
import org.babyfish.jimmer.ksp.immutable.meta.ImmutableType
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTree
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel

/**
 * @author Enaium
 */
class KotlinPropsChoosePanel(rootImmutableType: ImmutableType) : JPanel() {

    private val root = ImmutableNode(rootImmutableType)

    init {
        layout = BorderLayout()
        val treeModel = DefaultTreeModel(root)
        val tree = Tree(treeModel)
//        tree.cellRenderer = ImmutableNodeCellRender()
        tree.addTreeWillExpandListener(object : TreeWillExpandListener {
            override fun treeWillExpand(event: TreeExpansionEvent) {
                val node = event.path.lastPathComponent as DefaultMutableTreeNode
                node.removeAllChildren()
                if (node.childCount == 0) {
                    when (node) {
                        is ImmutableNode -> {
                            node.immutableType.properties.forEach {
                                node.add(ImmutablePropNode(it.value))
                            }
                        }

                        is ImmutablePropNode -> {
                            node.immutableProp.targetType?.properties?.forEach {
                                node.add(ImmutablePropNode(it.value))
                            }
                        }
                    }
                }
                treeModel.nodeStructureChanged(node)
            }

            override fun treeWillCollapse(event: TreeExpansionEvent) {
            }
        })
        tree.collapseRow(0)
        add(JScrollPane(tree), BorderLayout.CENTER)
    }

    private class ImmutableNodeCellRender : DefaultTreeCellRenderer() {
        override fun getTreeCellRendererComponent(
            tree: JTree,
            value: Any,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ): Component {
            icon = when (value) {
                is ImmutableNode -> AllIcons.Nodes.Interface
                is ImmutablePropNode -> AllIcons.Nodes.Property
                else -> null
            }
            text = value.toString()
            return this
        }
    }

    private class ImmutableNode(val immutableType: ImmutableType) : DefaultMutableTreeNode() {

        override fun isLeaf(): Boolean {
            return immutableType.properties.isEmpty()
        }

        override fun toString(): String {
            return immutableType.name
        }
    }

    private class ImmutablePropNode(val immutableProp: ImmutableProp) : DefaultMutableTreeNode() {

        override fun isLeaf(): Boolean {
            return immutableProp.targetType?.properties?.isEmpty() != false
        }

        override fun toString(): String {
            return immutableProp.name
        }
    }
}