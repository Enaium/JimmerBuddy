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
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.*
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
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
    private val onSelected: ((SmartPsiElementPointer<PsiElement>?) -> Unit)? = null
) : JPanel() {

    private val root = DefaultMutableTreeNode()
    private val tree = Tree(root)

    init {
        layout = BorderLayout()
        tree.isRootVisible = false
        tree.cellRenderer = NodeRenderer()

        tree.addTreeSelectionListener {
            tree.lastSelectedPathComponent?.also { select ->
                if (select is ImmutableNode) {
                    (select.target.element as? Navigatable)?.navigate(true)
                    onSelected?.invoke(select.target)
                }
            }
        }

        add(JBScrollPane(tree), BorderLayout.CENTER)
        buildTree()
    }

    private fun buildTree() {
        CoroutineScope(Dispatchers.Default).launch {
            supervisorScope {
                ReadAction.run<Throwable> {
                    val psiFile = file.toPsiFile(project) ?: return@run

                    when (psiFile) {
                        is PsiJavaFile -> {
                            val psiClass = psiFile.getChildOfType<PsiClass>() ?: return@run
                            val immutableType = ImmutableType(psiClass.createSmartPointer())
                            root.add(immutableType)
                            loadJavaProperties(psiClass, immutableType)
                        }

                        is KtFile -> {
                            val ktClass = psiFile.getChildOfType<KtClass>() ?: return@run
                            val immutableType = ImmutableType(ktClass.createSmartPointer())
                            root.add(immutableType)
                            loadKotlinProperties(ktClass, immutableType)
                        }
                    }
                }
            }
            withContext(Dispatchers.EDT) {
                (tree.model as DefaultTreeModel).nodeStructureChanged(root)
                expandAll()
            }
        }
    }

    private fun loadJavaProperties(psiClass: PsiClass, classNode: ImmutableType) {
        try {
            val immutableType = psiClass.toImmutable().toCommonImmutableType()
            immutableType.props().forEach { prop ->
                psiClass.methods.find { it.name == prop.name() }?.let { method ->
                    classNode.add(ImmutableProp(method.createSmartPointer()))
                }
            }
        } catch (_: Exception) {
            psiClass.methods
                .filter { it.parameterList.parametersCount == 0 && it.returnType != null }
                .filterNot { it.name in listOf("toString", "hashCode", "equals") }
                .forEach { classNode.add(ImmutableProp(it.createSmartPointer())) }
        }
    }

    private fun loadKotlinProperties(ktClass: KtClass, classNode: ImmutableType) {
        try {
            val immutableType = ktClass.toImmutable().toCommonImmutableType()
            immutableType.props().forEach { prop ->
                ktClass.getProperties().find { it.name == prop.name() }?.let { property ->
                    classNode.add(ImmutableProp(property.createSmartPointer()))
                }
            }
        } catch (_: Exception) {
            ktClass.getProperties().forEach { classNode.add(ImmutableProp(it.createSmartPointer())) }
        }
    }

    private fun expandAll() {
        var row = 0
        while (row < tree.rowCount) {
            tree.expandRow(row++)
        }
    }

    private open class ImmutableNode(val target: SmartPsiElementPointer<PsiElement>) :
        DefaultMutableTreeNode()

    private class ImmutableType(target: SmartPsiElementPointer<PsiElement>) : ImmutableNode(target) {
        override fun toString(): String = runReadOnly {
            when (val element = target.element) {
                is PsiClass -> element.name ?: ""
                is KtClass -> element.name ?: ""
                else -> element?.text ?: ""
            }
        }

        override fun isLeaf(): Boolean = false

        override fun equals(other: Any?): Boolean = other is ImmutableType && other.target == target
        override fun hashCode(): Int = target.hashCode()
    }

    private class ImmutableProp(target: SmartPsiElementPointer<PsiElement>) : ImmutableNode(target) {
        override fun toString(): String = runReadOnly {
            when (val element = target.element) {
                is PsiMethod -> element.name
                is KtProperty -> element.name ?: ""
                else -> element?.text ?: ""
            }
        }

        override fun isLeaf(): Boolean = true

        override fun equals(other: Any?): Boolean = other is ImmutableProp && other.target == target
        override fun hashCode(): Int = target.hashCode()
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
                        is ImmutableType -> icon = JimmerBuddy.Icons.IMMUTABLE
                        is ImmutableProp -> icon = JimmerBuddy.Icons.PROP
                    }
                }, BorderLayout.CENTER)
            }
        }
    }
}
