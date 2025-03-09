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

import cn.enaium.jimmer.buddy.dialog.NewDtoFileDialog
import cn.enaium.jimmer.buddy.utility.CommonImmutableType
import com.intellij.icons.AllIcons
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckboxTree.CheckboxTreeCellRenderer
import com.intellij.ui.CheckboxTreeListener
import com.intellij.ui.CheckedTreeNode
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTree
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * @author Enaium
 */
class ImmutablePropsChoosePanel(
    rootImmutableType: CommonImmutableType,
    val properties: MutableList<NewDtoFileDialog.DtoProperty>
) : JPanel() {

    private val root = ImmutableTypeNode(rootImmutableType)

    init {
        layout = BorderLayout()
        val tree = CheckboxTree(ImmutableNodeCellRender(), root)
        tree.isRootVisible = true
        tree.collapseRow(0)
        tree.addTreeWillExpandListener(object : TreeWillExpandListener {
            override fun treeWillExpand(event: TreeExpansionEvent) {
                val node = event.path.lastPathComponent as DefaultMutableTreeNode
                node.childCount > 0 && return
                node.removeAllChildren()

                fun addProps(immutableType: CommonImmutableType) {
                    immutableType.superTypes().forEach { superType ->
                        addProps(superType)
                    }
                    immutableType.properties().forEach {
                        node.add(ImmutablePropNode(it))
                    }
                }

                if (node.childCount == 0) {
                    when (node) {
                        is ImmutableTypeNode -> {
                            addProps(node.immutableType)
                        }

                        is ImmutablePropNode -> {
                            node.immutableProp.targetType()?.also {
                                addProps(it)
                            }
                        }
                    }
                }
                (tree.model as DefaultTreeModel).nodeStructureChanged(node)
            }

            override fun treeWillCollapse(event: TreeExpansionEvent) {
            }
        })
        tree.addCheckboxTreeListener(object : CheckboxTreeListener {
            override fun nodeStateChanged(node: CheckedTreeNode) {
                properties.clear()
                fun addProperty(property: NewDtoFileDialog.DtoProperty, immutableNode: ImmutableNode) {
                    val children = immutableNode.children()
                    while (children.hasMoreElements()) {
                        val node = children.nextElement() as ImmutableNode
                        if (node.choose()) {
                            property.properties.add(NewDtoFileDialog.DtoProperty(node.toString()))
                            addProperty(property, node)
                        }
                    }
                }
                root.children().toList().filterIsInstance<ImmutableNode>().filter { it.choose() }.forEach {
                    addProperty(NewDtoFileDialog.DtoProperty(it.toString()).also {
                        properties.add(it)
                    }, it)
                }
            }
        })
        add(JScrollPane(tree), BorderLayout.CENTER)
    }

    private class ImmutableNodeCellRender : CheckboxTreeCellRenderer() {
        override fun customizeRenderer(
            tree: JTree,
            value: Any,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ) {
            textRenderer.icon = when (value) {
                is ImmutableTypeNode -> AllIcons.Nodes.Interface
                is ImmutablePropNode -> if (leaf) {
                    AllIcons.Nodes.Property
                } else {
                    AllIcons.Nodes.Interface
                }

                else -> null
            }
            textRenderer.append(value.toString())
        }
    }

    private open class ImmutableNode() : CheckedTreeNode() {
        init {
            isChecked = false
        }

        fun choose(): Boolean {
            return isChecked() || children().toList().filterIsInstance<ImmutableNode>().any { it.isChecked() }
        }
    }

    private class ImmutableTypeNode(val immutableType: CommonImmutableType) : ImmutableNode() {
        override fun isLeaf(): Boolean {
            return immutableType.properties().isEmpty()
        }

        override fun toString(): String {
            return immutableType.name()
        }
    }

    private class ImmutablePropNode(val immutableProp: CommonImmutableType.CommonImmutableProp) : ImmutableNode() {
        override fun isLeaf(): Boolean {
            return immutableProp.targetType()?.properties()?.isEmpty() != false
        }

        override fun toString(): String {
            return immutableProp.name()
        }
    }
}