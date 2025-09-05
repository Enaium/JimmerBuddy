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
import cn.enaium.jimmer.buddy.dialog.GenerateDDLDialog
import cn.enaium.jimmer.buddy.dialog.NewDtoFileDialog
import cn.enaium.jimmer.buddy.utility.*
import cn.enaium.jimmer.buddy.utility.CommonImmutableType.CommonImmutableProp.Companion.type
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
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
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
import org.jetbrains.kotlin.psi.KtProperty
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
                                        GenerateDDLDialog(project, thread {
                                            runReadOnly {
                                                tree.selectionModel.selectionPaths.mapNotNull { selectionPath ->
                                                    val target =
                                                        (selectionPath.lastPathComponent as? ImmutableType)?.target
                                                    when (target) {
                                                        is PsiClass -> {
                                                            target.takeIf { it.isEntity() }?.toImmutable()
                                                                ?.toCommonImmutableType()
                                                        }

                                                        is KtClass -> {
                                                            target.takeIf { it.isEntity() }?.toImmutable()
                                                                ?.toCommonImmutableType()
                                                        }

                                                        else -> {
                                                            null
                                                        }
                                                    }
                                                }.toSet()
                                            }
                                        }).show()
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
                        if (project.isDumb()) {
                            return
                        }
                        loadImmutables(project)
                    }
                }, null, "Refresh", Dimension(24, 24)), BorderLayout.EAST)
            }, BorderLayout.NORTH
        )
        add(JBScrollPane(tree), BorderLayout.CENTER)
        project.runWhenSmart {
            loadImmutables(project)
        }
    }


    fun loadImmutables(project: Project) {
        CoroutineScope(Dispatchers.Default).launch {
            withBackgroundProgress(project, "Loading Immutables") {
                val results = mutableListOf<ImmutableType>()
                ReadAction.run<Throwable> {
                    val allKeys = FileBasedIndex.getInstance().getAllKeys(JimmerBuddy.Indexes.INTERFACE_CLASS, project)
                    if (project.isJavaProject()) {
                        allKeys.mapNotNull {
                            JavaPsiFacade.getInstance(project).findClass(it, project.projectScope())
                        }.filter { it.isImmutable() }
                            .mapNotNullTo(results) { psiClass ->
                                try {
                                    ImmutableType(psiClass).apply {
                                        psiClass.toImmutable().toCommonImmutableType().props().forEach {
                                            psiClass.methods.find { m -> m.name == it.name() }?.let { method ->
                                                add(ImmutableProp(method, it))
                                            }
                                        }
                                    }
                                } catch (e: Throwable) {
                                    JimmerBuddy.getWorkspace(project).log.error(e)
                                    null
                                }
                            }
                    } else if (project.isKotlinProject()) {
                        allKeys.mapNotNull {
                            KotlinFullClassNameIndex[it, project, project.projectScope()].firstOrNull() as? KtClass
                        }.filter { it.isImmutable() }
                            .mapNotNullTo(results) { ktClass ->
                                try {
                                    ImmutableType(ktClass).apply {
                                        ktClass.toImmutable().toCommonImmutableType().props().forEach {
                                            ktClass.getProperties().find { p -> p.name == it.name() }?.let { property ->
                                                add(ImmutableProp(property, it))
                                            }
                                        }
                                    }
                                } catch (e: Throwable) {
                                    JimmerBuddy.getWorkspace(project).log.error(e)
                                    null
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