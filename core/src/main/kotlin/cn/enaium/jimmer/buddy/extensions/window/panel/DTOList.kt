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
import cn.enaium.jimmer.buddy.utility.DTO_FILE
import cn.enaium.jimmer.buddy.utility.findProjects
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.core.util.toVirtualFile
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.file.Path
import javax.swing.*
import kotlin.io.path.nameWithoutExtension

class DTOList(project: Project) : JPanel() {
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
                GenerateProject.generate(
                    project.findProjects(),
                    setOf("main", "test"),
                    GenerateProject.SourceRootType.DTO
                ).forEach { (projectDir, sourceFiles, src) ->
                    sourceFiles.forEach { dto ->
                        names.add(DtoItem(dto.nameWithoutExtension, dto))
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
            add(JBScrollPane(dtoList), BorderLayout.CENTER)
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
            return JLabel(value!!.name, JimmerBuddy.Icons.DTO_FILE, SwingConstants.LEFT)
        }
    }
}