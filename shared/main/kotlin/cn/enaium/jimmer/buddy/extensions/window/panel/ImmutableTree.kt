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
import cn.enaium.jimmer.buddy.dialog.NewDtoFileDialog
import cn.enaium.jimmer.buddy.utility.findProjects
import cn.enaium.jimmer.buddy.utility.hasImmutableAnnotation
import cn.enaium.jimmer.buddy.utility.runReadActionSmart
import cn.enaium.jimmer.buddy.utility.runWhenSmart
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
import com.intellij.psi.PsiMethod
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.core.util.toVirtualFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import kotlin.io.path.extension
import kotlin.io.path.walk

/**
 * @author Enaium
 */
class ImmutableTree(val project: Project) : JPanel() {

    private val root = DefaultMutableTreeNode()
    private val tree = Tree(root)

    init {
        layout = BorderLayout()
        tree.isRootVisible = false
        tree.cellRenderer = ImmutableNodeCell()
        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                tree.lastSelectedPathComponent?.also { select ->

                    fun navigate() {
                        if (select is ImmutableNode) {
                            (select.target as Navigatable).navigate(true)
                        }
                    }

                    if (SwingUtilities.isRightMouseButton(e)) {
                        JBPopupMenu().apply {
                            add(JMenuItem("Go To").apply {
                                addActionListener {
                                    navigate()
                                }
                            })
                            if (select is ImmutableType) {
                                add(JMenuItem("New DTO").apply {
                                    addActionListener {
                                        NewDtoFileDialog(project, select.sourceFile, select.qualifiedName).show()
                                    }
                                })
                            }
                        }.show(tree, e.x, e.y)
                    } else if (e.clickCount == 2) {
                        if (select is ImmutableProp) {
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
                        loadImmutables()
                    }
                }, null, "Refresh", Dimension(24, 24)), BorderLayout.EAST)
            }, BorderLayout.NORTH
        )
        add(JBScrollPane(tree), BorderLayout.CENTER)
        loadImmutables()
    }

    fun loadImmutables() {
        project.runWhenSmart {
            CoroutineScope(Dispatchers.IO).launch {
                val files = mutableSetOf<File>()
                root.removeAllChildren()
                findProjects(project.guessProjectDir()!!.toNioPath()).forEach {
                    listOf("src/main/java", "src/test/java", "src/main/kotlin", "src/test/kotlin").forEach { src ->
                        it.resolve(src).walk().forEach { file ->
                            if (file.extension == "java" || file.extension == "kt") {
                                files.add(file.toFile())
                            }
                        }
                    }
                }
                files.forEach { file ->
                    if (file.extension == "java") {
                        project.runReadActionSmart {
                            file.toVirtualFile()?.findPsiFile(project)?.getChildOfType<PsiClass>()?.also { psiClass ->
                                if (psiClass.hasImmutableAnnotation()) {
                                    val newChild = ImmutableType(psiClass)
                                    psiClass.methods.forEach {
                                        newChild.add(ImmutableProp(it))
                                    }
                                    root.add(newChild)
                                }
                            }
                        }
                    } else if (file.extension == "kt") {
                        project.runReadActionSmart {
                            file.toPsiFile(project)?.getChildOfType<KtClass>()?.also { ktClass ->
                                if (ktClass.hasImmutableAnnotation()) {
                                    val newChild = ImmutableType(ktClass)
                                    ktClass.getProperties().forEach {
                                        newChild.add(ImmutableProp(it))
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


    private class ImmutableNodeCell() : DefaultTreeCellRenderer() {
        override fun getTreeCellRendererComponent(
            tree: JTree,
            value: Any,
            sel: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ): Component {

            if (value is ImmutableType) {
                icon = JimmerBuddy.Icons.IMMUTABLE
            } else if (value is ImmutableProp) {
                icon = JimmerBuddy.Icons.PROP
            }

            return JPanel(BorderLayout()).apply {
                if (sel) {
                    setBackground(this@ImmutableNodeCell.getBackgroundSelectionColor())
                } else {
                    setBackground(this@ImmutableNodeCell.getBackground())
                }
                add(JLabel(value.toString()).apply {
                    icon = this@ImmutableNodeCell.icon
                }, BorderLayout.CENTER)
            }
        }
    }

    private open class ImmutableNode(val target: PsiElement) :
        DefaultMutableTreeNode()

    private open class ImmutableType(target: PsiElement) : ImmutableNode(target) {
        val sourceFile = target.containingFile.virtualFile.toNioPath()
        val qualifiedName: String = if (target is PsiClass) {
            target.qualifiedName ?: "Unknown Name"
        } else if (target is KtClass) {
            target.fqName?.asString() ?: "Unknown Name"
        } else {
            target.text
        }

        override fun isLeaf(): Boolean {
            return false
        }

        override fun toString(): String {
            return if (target is PsiClass) {
                target.name ?: "Unknown Name"
            } else if (target is KtClass) {
                target.name ?: "Unknown Name"
            } else {
                target.text
            }
        }
    }

    private open class ImmutableProp(target: PsiElement) : ImmutableNode(target) {

        override fun isLeaf(): Boolean {
            return true
        }

        override fun toString(): String {
            return if (target is PsiMethod) {
                target.name
            } else if (target is KtProperty) {
                target.name ?: "Unknown Name"
            } else {
                target.text
            }
        }
    }
}