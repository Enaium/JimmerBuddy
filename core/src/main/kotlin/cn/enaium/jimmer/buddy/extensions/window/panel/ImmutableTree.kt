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
import cn.enaium.jimmer.buddy.extensions.index.ClassKindIndex
import cn.enaium.jimmer.buddy.utility.*
import cn.enaium.jimmer.buddy.utility.CommonImmutableProp.Companion.type
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
import com.intellij.pom.Navigatable
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.indexing.FileBasedIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer

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
                            (select.target.element as? Navigatable)?.navigate(true)
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
                                        NewDtoFileDialog(
                                            project,
                                            select.sourceFile ?: return@addActionListener,
                                            select.qualifiedName
                                        ).show()
                                    }
                                })
                                add(JMenuItem(I18n.message("toolwindow.buddy.menu.generateDDL")).apply {
                                    addActionListener {
                                        GenerateDDLDialog(project, thread {
                                            runReadOnly {
                                                tree.selectionModel.selectionPaths.mapNotNull { selectionPath ->
                                                    val target =
                                                        (selectionPath.lastPathComponent as? ImmutableType)?.target
                                                    when (val element = target?.element) {
                                                        is PsiClass -> {
                                                            element.takeIf { it.isEntity() }?.qualifiedName?.let {
                                                                CommonImmutableTypeCache.getInstance(
                                                                    project
                                                                ).get(it)
                                                            }
                                                        }

                                                        is KtClass -> {
                                                            element.takeIf { it.isEntity() }?.fqName?.asString()?.let {
                                                                CommonImmutableTypeCache.getInstance(project).get(it)
                                                            }
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
                }, null, "Refresh", Dimension(24, 24)), BorderLayout.WEST)
                add(tree.createSearchField { node, query ->
                    node is ImmutableType && node.matchesSearch(query)
                }, BorderLayout.CENTER)
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
        tree.showWaitingForIndexes()
        project.runWhenSmart {
            loadImmutables(project)
        }
    }


    fun loadImmutables(project: Project) {
        CoroutineScope(Dispatchers.Default).launch {
            withBackgroundProgress(project, "Loading Immutables") {
                val results = mutableListOf<ImmutableType>()

                ReadAction.run<Throwable> {
                    val index = FileBasedIndex.getInstance()
                    val classNames = index
                        .getAllKeys(JimmerBuddy.Indexes.CLASS_KIND, project)
                        .filter {
                            index.getValues(
                                JimmerBuddy.Indexes.CLASS_KIND,
                                it,
                                project.allScope()
                            ).contains(ClassKindIndex.Kind.IMMUTABLE)
                        }

                    classNames.forEach { qualifiedName ->
                        try {
                            val psiClass = JavaPsiFacade.getInstance(project)
                                .findClass(qualifiedName, project.allScope()) ?: return@forEach
                            val immutableType = when (val nav = psiClass.navigationElement) {
                                is KtClass -> ImmutableType(nav.createSmartPointer()).apply {
                                    CommonImmutableTypeCache.getInstance(project)
                                        .get(qualifiedName)?.props?.forEach { prop ->
                                            nav.getProperties().find { property -> property.name == prop.name }
                                                ?.also { property ->
                                                    add(ImmutableProp(property.createSmartPointer(), prop))
                                                }
                                        }
                                }
                                is PsiClass -> ImmutableType(nav.createSmartPointer()).apply {
                                    CommonImmutableTypeCache.getInstance(project)
                                        .get(qualifiedName)?.props?.forEach { prop ->
                                            nav.methods.find { method -> method.name == prop.name }
                                                ?.also { method ->
                                                    add(ImmutableProp(method.createSmartPointer(), prop))
                                                }
                                        }
                                }
                                else -> return@forEach
                            }
                            results.add(immutableType)
                        } catch (e: Throwable) {
                            JimmerBuddy.getWorkspace(project).log.error(e)
                        }
                    }
                }

                withContext(Dispatchers.EDT) {
                    root.removeAllChildren()
                    results.forEach { root.add(it) }
                    tree.nodeStructureChanged(root)
                    tree.showEmptyState()
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
                add(
                    tree.searchTextComponent(value.toString(), this@ImmutableNodeCell.icon, sel),
                    BorderLayout.CENTER
                )
            }
        }
    }

    private open class ImmutableNode(val target: SmartPsiElementPointer<PsiElement>) :
        DefaultMutableTreeNode()

    private open class ImmutableType(target: SmartPsiElementPointer<PsiElement>) : ImmutableNode(target) {
        val sourceFile = target.element?.containingFile?.virtualFile?.toNioPath()
        val qualifiedName: String = when (val element = target.element) {
            is PsiClass -> {
                element.qualifiedName ?: "Unknown Name"
            }

            is KtClass -> {
                element.fqName?.asString() ?: "Unknown Name"
            }

            else -> {
                element?.text ?: "Unknown Name"
            }
        }

        override fun isLeaf(): Boolean {
            return false
        }

        override fun toString(): String {
            return qualifiedName.substringAfterLast(".")
        }

        fun matchesSearch(query: String): Boolean {
            if (toString().matchesFuzzy(query)) {
                return true
            }
            return children().toList()
                .filterIsInstance<ImmutableProp>()
                .any { prop -> prop.prop.isAssociation && prop.prop.name.contains(query, ignoreCase = true) }
        }
    }

    private open class ImmutableProp(
        target: SmartPsiElementPointer<PsiElement>,
        val prop: CommonImmutableProp
    ) :
        ImmutableNode(target) {
        val name = when (val element = target.element) {
            is PsiMethod -> {
                element.name
            }

            is KtProperty -> {
                (element.name ?: "Unknown Name")
            }

            else -> {
                element?.text
            }
        }.let { name ->
            "$name: ${prop.simpleTypeName} (${prop.type().description})".let { typeName ->
                prop.targetType?.let { targetType -> "$typeName -> ${targetType.name}" } ?: typeName
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
