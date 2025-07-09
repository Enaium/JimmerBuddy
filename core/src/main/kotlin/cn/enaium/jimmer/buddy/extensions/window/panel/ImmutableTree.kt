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
import cn.enaium.jimmer.buddy.dialog.GenerateDDLDialog
import cn.enaium.jimmer.buddy.dialog.NewDtoFileDialog
import cn.enaium.jimmer.buddy.utility.*
import cn.enaium.jimmer.buddy.utility.CommonImmutableType.CommonImmutableProp.Companion.type
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
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel

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
                            add(JMenuItem(I18n.message("toolwindow.buddy.menu.goto")).apply {
                                addActionListener {
                                    navigate()
                                }
                            })
                            if (select is ImmutableType) {
                                add(JMenuItem(I18n.message("toolwindow.buddy.menu.newDto")).apply {
                                    addActionListener {
                                        NewDtoFileDialog(project, select.sourceFile, select.qualifiedName).show()
                                    }
                                })
                                add(JMenuItem(I18n.message("toolwindow.buddy.menu.generateDDL")).apply {
                                    addActionListener {
                                        val target = select.target
                                        val commonImmutableType = thread {
                                            runReadOnly {
                                                if (target is PsiClass) {
                                                    target.toImmutable().toCommonImmutableType()
                                                } else if (select.target is KtClass) {
                                                    target.toImmutable().toCommonImmutableType()
                                                } else {
                                                    null
                                                }
                                            }
                                        }
                                        commonImmutableType?.also {
                                            GenerateDDLDialog(project, it).show()
                                        }
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
                root.removeAllChildren()
                val projects = findProjects(project.guessProjectDir()!!.toNioPath())

                GenerateProject.generate(
                    projects,
                    setOf("main", "test"),
                    GenerateProject.SourceRootType.JAVA
                ).forEach { (_, sourceFiles, _) ->
                    sourceFiles.forEach { sourceFile ->
                        project.runReadActionSmart {
                            sourceFile.toFile().toVirtualFile()?.findPsiFile(project)?.getChildOfType<PsiClass>()
                                ?.also { psiClass ->
                                    if (psiClass.isImmutable()) {
                                        val newChild = ImmutableType(psiClass)
                                        try {
                                            psiClass.toImmutable().toCommonImmutableType().props().forEach {
                                                psiClass.methods.find { method -> method.name == it.name() }
                                                    ?.also { method ->
                                                        newChild.add(ImmutableProp(method, it))
                                                    }
                                            }
                                        } catch (e: Throwable) {
                                            JimmerBuddy.getWorkspace(project).log.error(e)
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
                                if (ktClass.isImmutable()) {
                                    try {
                                        val newChild = ImmutableType(ktClass)
                                        ktClass.toImmutable().toCommonImmutableType().props().forEach {
                                            ktClass.getProperties().find { property -> property.name == it.name() }
                                                ?.also { property ->
                                                    newChild.add(ImmutableProp(property, it))
                                                }
                                        }
                                        root.add(newChild)
                                    } catch (e: Throwable) {
                                        JimmerBuddy.getWorkspace(project).log.error(e)
                                    }
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
        val qualifiedName: String = when (target) {
            is PsiClass -> {
                target.qualifiedName ?: "Unknown Name"
            }

            is KtClass -> {
                target.fqName?.asString() ?: "Unknown Name"
            }

            else -> {
                target.text
            }
        }

        override fun isLeaf(): Boolean {
            return false
        }

        override fun toString(): String {
            return qualifiedName.substringAfterLast(".")
        }
    }

    private open class ImmutableProp(target: PsiElement, val prop: CommonImmutableType.CommonImmutableProp) :
        ImmutableNode(target) {
        val name = when (target) {
            is PsiMethod -> {
                target.name
            }

            is KtProperty -> {
                (target.name ?: "Unknown Name")
            }

            else -> {
                target.text
            }
        }.let { name ->
            "$name: ${prop.simpleTypeName()} (${prop.type().description})".let { typeName ->
                prop.targetType()?.let { targetType -> "$typeName -> ${targetType.name()}" } ?: typeName
            }
        }

        override fun isLeaf(): Boolean {
            return true
        }

        override fun toString(): String {
            return name
        }
    }
}