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

package cn.enaium.jimmer.buddy.action

import cn.enaium.jimmer.buddy.database.model.Table
import cn.enaium.jimmer.buddy.database.provider.IntelliJDatabaseMetadataProvider
import cn.enaium.jimmer.buddy.dialog.GenerateEntityDialog
import cn.enaium.jimmer.buddy.utility.I18n
import com.intellij.database.model.DasTable
import com.intellij.database.psi.DbElement
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project

/**
 * Action that appears in the Database Tools tree context menu.
 * When right-clicking on a table, it extracts the table metadata via
 * com.intellij.database API and opens the GenerateEntityDialog.
 *
 * This action is only registered when the com.intellij.database plugin is available
 * (via the optional config file jimmerBuddy-databasePlugin.xml).
 *
 * @author Enaium
 */
class GenerateFromDbTableAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val element = e.getData(CommonDataKeys.PSI_ELEMENT)
            ?: getSelectedElementFromTree(e)
            ?: return

        val dasTable = unwrapToDasTable(element) ?: return
        val table = IntelliJDatabaseMetadataProvider().extractTableFromDasTable(dasTable) ?: return
        GenerateEntityDialog(project, setOf(table)).show()
    }

    private fun getSelectedElementFromTree(e: AnActionEvent): Any? {
        return try {
            val tree = e.inputEvent?.component as? javax.swing.JTree ?: return null
            val selectionPath = tree.selectionPath ?: return null
            val lastPathComponent = selectionPath.lastPathComponent
            if (lastPathComponent is javax.swing.tree.DefaultMutableTreeNode) {
                lastPathComponent.userObject
            } else {
                lastPathComponent
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null || project.isDisposed) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val psiElement = e.getData(CommonDataKeys.PSI_ELEMENT)
        val isVisible = psiElement != null && isDbTableElement(psiElement)
        e.presentation.isEnabledAndVisible = isVisible
        e.presentation.text = I18n.message("database.tool.generateEntity")
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    companion object {
        fun isDbTableElement(obj: Any): Boolean {
            return obj is DasTable || unwrapToDasTable(obj) != null
        }

        /**
         * Unwraps a [DbElement] wrapper to get the underlying [DasTable].
         * Database Tools tree nodes may provide [DbElement] instances that wrap [DasObject] via [DbElement.getDelegate].
         */
        private fun unwrapToDasTable(obj: Any): DasTable? {
            if (obj is DasTable) return obj
            if (obj is DbElement) {
                val delegate = obj.delegate
                if (delegate is DasTable) return delegate
            }
            return null
        }

        fun extractTableFromDbElement(obj: Any): Table? {
            val dasTable = unwrapToDasTable(obj) ?: return null
            return IntelliJDatabaseMetadataProvider().extractTableFromDasTable(dasTable)
        }
    }
}