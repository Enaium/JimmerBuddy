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

package cn.enaium.jimmer.buddy.extensions.window

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.utility.findProjects
import cn.enaium.jimmer.buddy.utility.isJimmerImmutableType
import com.intellij.icons.AllIcons
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFileFactory
import com.intellij.ui.components.JBList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.core.util.toVirtualFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.file.Path
import javax.swing.*
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.walk

/**
 * @author Enaium
 */
class BuddyToolWindow : ToolWindowFactory {
    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow
    ) {
        toolWindow.contentManager.addContent(
            toolWindow.contentManager.factory.createContent(
                ImmutableList(project),
                "Immutables",
                false
            )
        )
        toolWindow.contentManager.addContent(
            toolWindow.contentManager.factory.createContent(
                DTOList(project),
                "DTOs",
                false
            )
        )
    }

    private class ImmutableList(project: Project) : JPanel() {
        init {
            layout = BorderLayout()

            val immutableList = JBList<ImmutableItem>().apply {
                cellRenderer = ImmutableCell()
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        if (e.clickCount == 2) {
                            selectedValue?.also {
                                FileEditorManager.getInstance(project).openFile(
                                    it.file.toFile().toVirtualFile()!!,
                                    true
                                )
                            }
                        }
                    }
                })
            }

            fun loadImmutables() {
                ApplicationManager.getApplication().executeOnPooledThread {
                    val names = mutableListOf<ImmutableItem>()
                    ApplicationManager.getApplication().runReadAction {
                        if (DumbService.isDumb(project)) return@runReadAction
                        findProjects(project.guessProjectDir()!!.toNioPath()).forEach {
                            listOf("src/main/java", "src/main/kotlin").forEach { src ->
                                it.resolve(src).toFile().walk().forEach { file ->
                                    if (file.extension == "java") {
                                        PsiFileFactory.getInstance(project).createFileFromText(
                                            file.name,
                                            JavaFileType.INSTANCE,
                                            file.readText()
                                        ).getChildOfType<PsiClass>()?.also { psiClass ->
                                            if (psiClass.isJimmerImmutableType()) {
                                                names.add(ImmutableItem(psiClass.name!!, file.toPath()))
                                            }
                                        }
                                    } else if (file.extension == "kt") {
                                        file.toPsiFile(project)?.getChildOfType<KtClass>()?.also { ktClass ->
                                            if (ktClass.isJimmerImmutableType()) {
                                                names.add(ImmutableItem(ktClass.name!!, file.toPath()))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ApplicationManager.getApplication().invokeLater {
                        immutableList.setListData(names.toTypedArray())
                    }
                }
            }
            loadImmutables()

            add(JPanel(BorderLayout()).apply {
                add(
                    JPanel(BorderLayout()).apply {
                        add(ActionButton(object : AnAction(AllIcons.Actions.Refresh) {
                            override fun actionPerformed(e: AnActionEvent) {
                                ApplicationManager.getApplication().runReadAction {
                                    loadImmutables()
                                }
                            }
                        }, null, "Refresh", Dimension(24, 24)), BorderLayout.EAST)
                    }, BorderLayout.NORTH
                )
                add(immutableList, BorderLayout.CENTER)
            }, BorderLayout.CENTER)
        }

        private data class ImmutableItem(val name: String, val file: Path)

        private class ImmutableCell : ListCellRenderer<ImmutableItem> {
            override fun getListCellRendererComponent(
                list: JList<out ImmutableItem?>?,
                value: ImmutableItem?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                return JLabel(value!!.name, JimmerBuddy.Icons.IMMUTABLE, SwingConstants.LEFT)
            }
        }
    }

    private class DTOList(project: Project) : JPanel() {
        init {
            layout = BorderLayout()

            val dtoList = JBList<DtoItem>().apply {
                cellRenderer = DtoCell()
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        if (e.clickCount == 2) {
                            selectedValue?.also {
                                FileEditorManager.getInstance(project).openFile(
                                    it.file.toFile().toVirtualFile()!!,
                                    true
                                )
                            }
                        }
                    }
                })
            }

            fun loadDTOs() {
                CoroutineScope(Dispatchers.IO).launch {
                    val names = mutableListOf<DtoItem>()
                    findProjects(project.guessProjectDir()!!.toNioPath()).forEach {
                        it.resolve("src/main/dto").walk().forEach { dto ->
                            if (dto.extension == "dto") {
                                names.add(DtoItem(dto.nameWithoutExtension, dto))
                            }
                        }
                    }
                    dtoList.setListData(names.toTypedArray())
                }
            }
            ApplicationManager.getApplication().runReadAction {
                loadDTOs()
            }

            add(JPanel(BorderLayout()).apply {
                add(
                    JPanel(BorderLayout()).apply {
                        add(ActionButton(object : AnAction(AllIcons.Actions.Refresh) {
                            override fun actionPerformed(e: AnActionEvent) {
                                ApplicationManager.getApplication().runReadAction {
                                    loadDTOs()
                                }
                            }
                        }, null, "Refresh", Dimension(24, 24)), BorderLayout.EAST)
                    }, BorderLayout.NORTH
                )
                add(dtoList, BorderLayout.CENTER)
            }, BorderLayout.CENTER)
        }

        private data class DtoItem(val name: String, val file: Path)

        private class DtoCell : ListCellRenderer<DtoItem> {
            override fun getListCellRendererComponent(
                list: JList<out DtoItem?>?,
                value: DtoItem?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                return JLabel(value!!.name, JimmerBuddy.Icons.DTO, SwingConstants.LEFT)
            }
        }
    }

    override fun isApplicable(project: Project): Boolean {
        return JimmerBuddy.isJimmerProject(project)
    }

    override fun shouldBeAvailable(project: Project): Boolean {
        return JimmerBuddy.isJimmerProject(project)
    }
}