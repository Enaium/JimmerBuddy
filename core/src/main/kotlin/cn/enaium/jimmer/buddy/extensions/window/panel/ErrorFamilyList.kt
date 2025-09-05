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

package cn.enaium.jimmer.buddy.extensions.window.panel

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.utility.*
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.withBackgroundProgress
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.pom.Navigatable
import com.intellij.psi.*
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.indexing.FileBasedIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.base.util.projectScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel

/**
 * @author Enaium
 */
class ErrorFamilyList(val project: Project) : JPanel() {

    private val root = DefaultMutableTreeNode()
    private val tree = Tree(root)

    init {
        layout = BorderLayout()
        tree.isRootVisible = false
        tree.cellRenderer = ErrorFamilyNodeCell()
        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                tree.lastSelectedPathComponent?.also { select ->

                    fun navigate() {
                        if (select is ErrorFamilyNode) {
                            (select.target as Navigatable).navigate(true)
                        }
                    }

                    if (SwingUtilities.isRightMouseButton(e)) {
                        JBPopupMenu().apply {
                            add(JMenuItem(I18n.message("toolwindow.buddy.menu.goto")).apply {
                                addActionListener {
                                    navigate()
                                }
                            })
                        }.show(tree, e.x, e.y)
                    } else if (e.clickCount == 2) {
                        if (select is ErrorFamilyNode) {
                            navigate()
                        }
                    }
                }
            }
        })
        add(
            JPanel(BorderLayout()).apply {
                add(ActionButton(object : AnAction(AllIcons.Actions.Refresh) {
                    override fun actionPerformed(e: AnActionEvent) {
                        if (project.isDumb()) {
                            return
                        }
                        loadErrorFamilies()
                    }
                }, null, "Refresh", Dimension(24, 24)), BorderLayout.EAST)
            }, BorderLayout.NORTH
        )
        add(JBScrollPane(tree), BorderLayout.CENTER)
        project.runWhenSmart {
            loadErrorFamilies()
        }
    }

    fun loadErrorFamilies() {
        CoroutineScope(Dispatchers.Default).launch {
            withBackgroundProgress(project, "Loading Error Families") {
                val results = mutableListOf<ErrorFamilyType>()
                ReadAction.run<Throwable> {
                    val allKeys = FileBasedIndex.getInstance().getAllKeys(JimmerBuddy.Indexes.ENUM_CLASS, project)
                    if (project.isJavaProject()) {
                        allKeys.mapNotNull { JavaPsiFacade.getInstance(project).findClass(it, project.projectScope()) }
                            .filter { it.isErrorFamily() }.mapTo(results) { psiClass ->
                                ErrorFamilyType(psiClass).apply {
                                    psiClass.getChildrenOfType<PsiEnumConstant>().forEach {
                                        add(ErrorFamilyField(it))
                                    }
                                }
                            }
                    } else if (project.isKotlinProject()) {
                        allKeys.mapNotNull { KotlinFullClassNameIndex[it, project, project.projectScope()].firstOrNull() as? KtClass }
                            .filter { it.isErrorFamily() }.mapTo(results) { ktClass ->
                                ErrorFamilyType(ktClass).apply {
                                    ktClass.getChildOfType<KtClassBody>()?.getChildrenOfType<KtEnumEntry>()?.forEach {
                                        add(ErrorFamilyField(it))
                                    }
                                }
                            }
                    }
                }

                withContext(Dispatchers.EDT) {
                    root.removeAllChildren()
                    results.forEach { root.add(it) }
                    (tree.model as DefaultTreeModel).nodeStructureChanged(root)
                }
            }
        }
    }

    private class ErrorFamilyNodeCell() : DefaultTreeCellRenderer() {
        override fun getTreeCellRendererComponent(
            tree: JTree,
            value: Any,
            sel: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ): Component {

            if (value is ErrorFamilyType) {
                icon = AllIcons.Nodes.Enum
            } else if (value is ErrorFamilyField) {
                icon = AllIcons.Nodes.ExceptionClass
            }

            return JPanel(BorderLayout()).apply {
                if (sel) {
                    setBackground(this@ErrorFamilyNodeCell.getBackgroundSelectionColor())
                } else {
                    setBackground(this@ErrorFamilyNodeCell.getBackground())
                }
                add(JLabel(runReadOnly { value.toString() }).apply {
                    icon = this@ErrorFamilyNodeCell.icon
                }, BorderLayout.CENTER)
            }
        }
    }

    private open class ErrorFamilyNode(val target: PsiElement) :
        DefaultMutableTreeNode()

    private open class ErrorFamilyType(target: PsiElement) : ErrorFamilyNode(target) {
        override fun isLeaf(): Boolean {
            return false
        }

        override fun toString(): String {
            return when (target) {
                is PsiClass -> {
                    target.name ?: "Unknown Name"
                }

                is KtClass -> {
                    target.name ?: "Unknown Name"
                }

                else -> {
                    target.text
                }
            }
        }
    }

    private open class ErrorFamilyField(target: PsiElement) : ErrorFamilyNode(target) {
        override fun isLeaf(): Boolean {
            return true
        }

        override fun toString(): String {
            return when (target) {
                is PsiField -> {
                    target.name
                }

                is KtProperty -> {
                    target.name ?: "Unknown Name"
                }

                else -> {
                    target.text
                }
            }
        }
    }
}