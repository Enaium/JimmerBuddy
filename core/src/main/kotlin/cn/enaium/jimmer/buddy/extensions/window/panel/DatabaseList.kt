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
import cn.enaium.jimmer.buddy.dialog.AddDatabaseDialog
import cn.enaium.jimmer.buddy.dialog.GenerateEntityDialog
import cn.enaium.jimmer.buddy.dialog.TypeMappingDialog
import cn.enaium.jimmer.buddy.storage.JimmerBuddySetting
import cn.enaium.jimmer.buddy.storage.JimmerBuddySetting.DatabaseItem
import cn.enaium.jimmer.buddy.utility.I18n
import cn.enaium.jimmer.buddy.utility.invokeLater
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

/**
 * @author Enaium
 */
class DatabaseList(val project: Project) : JPanel() {
    init {
        layout = BorderLayout()
        val databaseList = JBList<DatabaseItem>()
        fun refresh() {
            invokeLater {
                databaseList.setListData(JimmerBuddySetting.INSTANCE.state.databases.toTypedArray())
            }
        }
        refresh()
        databaseList.apply {
            cellRenderer = DatabaseCell()
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    val select = selectedValue ?: return
                    if (SwingUtilities.isRightMouseButton(e)) {
                        JBPopupMenu().apply {
                            add(JMenuItem(I18n.message("toolwindow.buddy.menu.generate")).apply {
                                addActionListener {
                                    GenerateEntityDialog(project, select).show()
                                }
                            })
                            add(JMenuItem(I18n.message("toolwindow.buddy.menu.edit")).apply {
                                addActionListener {
                                    if (AddDatabaseDialog(select).showAndGet()) {
                                        JimmerBuddySetting.INSTANCE.state.databases =
                                            JimmerBuddySetting.INSTANCE.state.databases - select
                                    }
                                    refresh()
                                }
                            })
                            add(JMenuItem(I18n.message("toolwindow.buddy.menu.remove")).apply {
                                addActionListener {
                                    JimmerBuddySetting.INSTANCE.state.databases =
                                        JimmerBuddySetting.INSTANCE.state.databases - select
                                    refresh()
                                }
                            })
                        }.show(this@apply, e.x, e.y)
                    }
                }
            })
        }
        add(
            JPanel(BorderLayout()).apply {
                add(JPanel().apply {
                    add(ActionButton(object : AnAction(AllIcons.Actions.Refresh) {
                        override fun actionPerformed(e: AnActionEvent) {
                            refresh()
                        }
                    }, null, "Refresh", Dimension(24, 24)))
                    add(ActionButton(object : AnAction(AllIcons.General.Add) {
                        override fun actionPerformed(e: AnActionEvent) {
                            if (AddDatabaseDialog().showAndGet()) {
                                refresh()
                            }
                        }
                    }, null, "Add", Dimension(24, 24)))
                    add(ActionButton(object : AnAction(AllIcons.General.Settings) {
                        override fun actionPerformed(e: AnActionEvent) {
                            TypeMappingDialog().show()
                        }
                    }, null, "Setting", Dimension(24, 24)))
                }, BorderLayout.EAST)
            }, BorderLayout.NORTH
        )
        add(JBScrollPane(databaseList))
    }

    private class DatabaseCell : ListCellRenderer<DatabaseItem> {
        override fun getListCellRendererComponent(
            list: JList<out DatabaseItem>,
            value: DatabaseItem,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            return JLabel(value.uri, JimmerBuddy.Icons.Database.DB, SwingConstants.LEFT)
        }
    }
}