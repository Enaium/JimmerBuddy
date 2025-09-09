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
import cn.enaium.jimmer.buddy.JimmerBuddy.GenerateProject
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoType
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiRoot
import cn.enaium.jimmer.buddy.utility.*
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.withBackgroundProgress
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.core.util.toVirtualFile
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import kotlin.io.path.nameWithoutExtension

class DTOList(val project: Project) : JPanel() {
    private val root = DefaultMutableTreeNode()
    private val tree = Tree(root)

    init {
        layout = BorderLayout()
        tree.isRootVisible = false
        tree.cellRenderer = DtoNodeCell()
        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                tree.lastSelectedPathComponent?.also { select ->

                    fun navigate() {
                        if (select is DtoNode) {
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
                        if (select is DtoType) {
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
                        loadDTOs()
                    }
                }, null, "Refresh", Dimension(24, 24)), BorderLayout.WEST)
                add(ActionButton(object : AnAction(AllIcons.Actions.More) {
                    override fun actionPerformed(e: AnActionEvent) {
                        val sourceComponent = (e.inputEvent?.source as? Component) ?: return
                        JBPopupFactory.getInstance()
                            .createActionGroupPopup(
                                I18n.message("toolwindow.buddy.menu.sortBy"),
                                DefaultActionGroup(
                                    listOf(
                                        object : AnAction(I18n.message("toolwindow.buddy.menu.sortBy.name")) {
                                            override fun actionPerformed(e: AnActionEvent) {
                                                tree.sortByName(root)
                                            }
                                        },
                                        object : AnAction(I18n.message("toolwindow.buddy.menu.sortBy.childCount")) {
                                            override fun actionPerformed(e: AnActionEvent) {
                                                tree.sortByChildCount(root)
                                            }
                                        }
                                    )
                                ),
                                e.dataContext,
                                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                                true
                            )
                            .showUnderneathOf(sourceComponent)
                    }
                }, null, "Menu", Dimension(24, 24)), BorderLayout.EAST)
            }, BorderLayout.NORTH
        )
        add(JBScrollPane(tree), BorderLayout.CENTER)
        project.runWhenSmart {
            loadDTOs()
        }
    }

    fun loadDTOs() {
        CoroutineScope(Dispatchers.Default).launch {
            withBackgroundProgress(project, "Loading DTOs") {
                val results = mutableListOf<DtoNode>()
                val projects = project.findProjects()
                GenerateProject.generate(
                    projects,
                    GenerateProject.SourceRootType.DTO
                ).forEach { (_, sourceFiles, _) ->
                    ReadAction.run<Throwable> {
                        sourceFiles.mapNotNullTo(results) { sourceFile ->
                            sourceFile.toFile().toVirtualFile()?.findPsiFile(project)?.getChildOfType<DtoPsiRoot>()
                                ?.let { root ->
                                    DtoFile(root).apply {
                                        root.dtoTypes.forEach {
                                            add(DtoType(it))
                                        }
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

            if (value is DtoFile) {
                icon = JimmerBuddy.Icons.DTO_FILE
            } else if (value is DtoType) {
                icon = JimmerBuddy.Icons.Nodes.DTO_TYPE
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

    private open class DtoNode(val target: PsiElement) :
        DefaultMutableTreeNode()

    private open class DtoFile(target: PsiElement) : DtoNode(target) {
        val sourceFile = target.containingFile.virtualFile.toNioPath()
        override fun isLeaf(): Boolean {
            return false
        }

        override fun toString(): String {
            return sourceFile.nameWithoutExtension
        }
    }

    private open class DtoType(target: PsiElement) : DtoNode(target) {
        override fun isLeaf(): Boolean {
            return true
        }

        override fun toString(): String {
            return if (target is DtoPsiDtoType) {
                target.name?.text ?: "Unknown Name"
            } else {
                target.text
            }
        }
    }
}