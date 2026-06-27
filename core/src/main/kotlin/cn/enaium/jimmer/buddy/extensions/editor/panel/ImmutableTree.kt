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

package cn.enaium.jimmer.buddy.extensions.editor.panel

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.utility.*
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel

/**
 * @author Enaium
 */
class ImmutableTree(
    private val project: Project,
    private val file: VirtualFile,
    private val onSelected: ((PsiElement?) -> Unit)? = null
) : JPanel() {

    private val root = DefaultMutableTreeNode()
    private val tree = Tree(root)

    init {
        layout = BorderLayout()
        tree.isRootVisible = false
        tree.cellRenderer = NodeRenderer()

        tree.addTreeSelectionListener {
            ReadAction.run<Throwable> {
                val element = when (val node = tree.lastSelectedPathComponent) {
                    is ClassNode -> node.element
                    is PropertyNode -> node.element
                    else -> null
                }
                onSelected?.invoke(element)
            }
        }

        add(JBScrollPane(tree), BorderLayout.CENTER)
        buildTree()
    }

    private fun buildTree() {
        ReadAction.run<Throwable> {
            val psiFile = file.toPsiFile(project) ?: return@run

            when (psiFile) {
                is PsiJavaFile -> {
                    val psiClass = psiFile.getChildOfType<PsiClass>() ?: return@run
                    val classNode = ClassNode(psiClass)
                    root.add(classNode)
                    loadJavaProperties(psiClass, classNode)
                }

                is KtFile -> {
                    val ktClass = psiFile.getChildOfType<KtClass>() ?: return@run
                    val classNode = ClassNode(ktClass)
                    root.add(classNode)
                    loadKotlinProperties(ktClass, classNode)
                }
            }
        }

        (tree.model as DefaultTreeModel).nodeStructureChanged(root)
        expandAll()
    }

    private fun loadJavaProperties(psiClass: PsiClass, classNode: ClassNode) {
        try {
            val immutableType = psiClass.toImmutable().toCommonImmutableType()
            immutableType.props().forEach { prop ->
                psiClass.methods.find { it.name == prop.name() }?.let { method ->
                    classNode.add(PropertyNode(method))
                }
            }
        } catch (_: Exception) {
            psiClass.methods
                .filter { it.parameterList.parametersCount == 0 && it.returnType != null }
                .filterNot { it.name in listOf("toString", "hashCode", "equals") }
                .forEach { classNode.add(PropertyNode(it)) }
        }
    }

    private fun loadKotlinProperties(ktClass: KtClass, classNode: ClassNode) {
        try {
            val immutableType = ktClass.toImmutable().toCommonImmutableType()
            immutableType.props().forEach { prop ->
                ktClass.getProperties().find { it.name == prop.name() }?.let { property ->
                    classNode.add(PropertyNode(property))
                }
            }
        } catch (_: Exception) {
            ktClass.getProperties().forEach { classNode.add(PropertyNode(it)) }
        }
    }

    private fun expandAll() {
        var row = 0
        while (row < tree.rowCount) {
            tree.expandRow(row++)
        }
    }

    private class ClassNode(val element: PsiElement) : DefaultMutableTreeNode(element) {
        override fun toString(): String = runReadOnly {
            when (element) {
                is PsiClass -> element.name ?: ""
                is KtClass -> element.name ?: ""
                else -> element.text
            }
        }

        override fun isLeaf(): Boolean = false

        override fun equals(other: Any?): Boolean = other is ClassNode && other.element == element
        override fun hashCode(): Int = element.hashCode()
    }

    private class PropertyNode(val element: PsiElement) : DefaultMutableTreeNode(element) {
        override fun toString(): String = runReadOnly {
            when (element) {
                is PsiMethod -> element.name
                is KtProperty -> element.name ?: ""
                else -> element.text
            }
        }

        override fun isLeaf(): Boolean = true

        override fun equals(other: Any?): Boolean = other is PropertyNode && other.element == element
        override fun hashCode(): Int = element.hashCode()
    }

    private class NodeRenderer : DefaultTreeCellRenderer() {
        override fun getTreeCellRendererComponent(
            tree: JTree,
            value: Any,
            sel: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ): Component {
            return JPanel(BorderLayout()).apply {
                if (sel) {
                    setBackground(this@NodeRenderer.getBackgroundSelectionColor())
                } else {
                    setBackground(this@NodeRenderer.getBackground())
                }
                add(JLabel(value.toString()).apply {
                    when (value) {
                        is ClassNode -> icon = JimmerBuddy.Icons.IMMUTABLE
                        is PropertyNode -> icon = JimmerBuddy.Icons.PROP
                    }
                }, BorderLayout.CENTER)
            }
        }
    }
}
