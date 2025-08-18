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

import cn.enaium.jimmer.buddy.JimmerBuddy.GenerateProject
import cn.enaium.jimmer.buddy.utility.*
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.core.util.toVirtualFile
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
                        loadErrorFamilies()
                    }
                }, null, "Refresh", Dimension(24, 24)), BorderLayout.EAST)
            }, BorderLayout.NORTH
        )
        add(JBScrollPane(tree), BorderLayout.CENTER)
        loadErrorFamilies()
    }

    fun loadErrorFamilies() {
        project.runWhenSmart {
            CoroutineScope(Dispatchers.IO).launch {
                root.removeAllChildren()
                val projects = project.findProjects()

                GenerateProject.generate(
                    projects,
                    setOf("main", "test"),
                    GenerateProject.SourceRootType.JAVA
                ).forEach { (_, sourceFiles, _) ->
                    sourceFiles.forEach { sourceFile ->
                        project.runReadActionSmart {
                            sourceFile.toFile().toVirtualFile()?.findPsiFile(project)?.getChildOfType<PsiClass>()
                                ?.also { psiClass ->
                                    if (psiClass.isErrorFamily()) {
                                        val newChild = ErrorFamilyType(psiClass)
                                        psiClass.getChildrenOfType<PsiEnumConstant>().forEach {
                                            newChild.add(ErrorFamilyField(it))
                                        }
                                        root.add(newChild)
                                    }
                                }
                        }
                    }
                }

                GenerateProject.generate(
                    projects,
                    setOf("main", "test"),
                    GenerateProject.SourceRootType.KOTLIN
                ).forEach { (_, sourceFiles, _) ->
                    sourceFiles.forEach { sourceFile ->
                        project.runReadActionSmart {
                            sourceFile.toFile().toPsiFile(project)?.getChildOfType<KtClass>()?.also { ktClass ->
                                if (ktClass.isErrorFamily()) {
                                    val newChild = ErrorFamilyType(ktClass)
                                    ktClass.getChildOfType<KtClassBody>()?.getChildrenOfType<KtEnumEntry>()?.forEach {
                                        newChild.add(ErrorFamilyField(it))
                                    }
                                    root.add(newChild)
                                }
                            }
                        }
                    }
                }
                project.runReadActionSmart { (tree.model as DefaultTreeModel).nodeStructureChanged(root) }
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