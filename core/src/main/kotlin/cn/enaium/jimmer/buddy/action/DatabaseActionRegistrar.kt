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

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.Constraints
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * Registers the [GenerateFromDbTableAction] into the [DatabaseViewPopupMenu] group
 * at runtime, after the Database Tools plugin has loaded its action groups.
 *
 * This is necessary because the [DatabaseViewPopupMenu] group is defined in the
 * [com.intellij.database] plugin's own plugin.xml, which is loaded at runtime.
 * Using [add-to-group] in the optional config XML would cause a compile-time
 * warning since the group is not available during compilation.
 *
 * The registrar is only registered via [jimmerBuddy-databasePlugin.xml], which is
 * loaded only when the [com.intellij.database] plugin is available.
 *
 * @author Enaium
 */
class DatabaseActionRegistrar : ProjectActivity {
    override suspend fun execute(project: Project) {
        try {
            val actionManager = ActionManager.getInstance()
            val group = actionManager.getAction("DatabaseViewPopupMenu")
            if (group is DefaultActionGroup) {
                val action = actionManager.getAction("JimmerBuddy.GenerateFromDbTable")
                if (action != null && group.childActionsOrStubs.none { it === action }) {
                    group.addAction(action, Constraints.FIRST)
                }
            }
        } catch (_: Exception) {
            // Database Tools plugin not available
        }
    }
}