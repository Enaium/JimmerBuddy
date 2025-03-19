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

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.dialog.NewDtoFileDialog
import cn.enaium.jimmer.buddy.utility.CommonImmutableType
import cn.enaium.jimmer.buddy.utility.runReadOnly
import cn.enaium.jimmer.buddy.utility.thread
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
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
    val project: Project,
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
                thread {
                    return@thread runReadOnly {
                        if (DumbService.isDumb(project)) return@runReadOnly null
                        val node = event.path.lastPathComponent as DefaultMutableTreeNode
                        node.childCount > 0 && return@runReadOnly null
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
                        return@runReadOnly node
                    }
                }?.also {
                    (tree.model as DefaultTreeModel).nodeStructureChanged(it)
                }
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
                            val element = NewDtoFileDialog.DtoProperty(node.toString())
                            property.properties.add(element)
                            addProperty(element, node)
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
                is ImmutableTypeNode -> JimmerBuddy.Icons.IMMUTABLE
                is ImmutablePropNode -> if (leaf) {
                    JimmerBuddy.Icons.PROP
                } else {
                    JimmerBuddy.Icons.IMMUTABLE
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

        fun choose(parent: ImmutableNode = this): Boolean {
            if (parent.isChecked()) {
                return true
            }
            val children = parent.children()
            while (children.hasMoreElements()) {
                val node = children.nextElement() as ImmutableNode
                if (choose(node)) {
                    return true
                }
            }
            return false
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