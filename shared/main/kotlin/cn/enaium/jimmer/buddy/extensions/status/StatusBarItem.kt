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

package cn.enaium.jimmer.buddy.extensions.status

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.utility.isJimmerProject
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup

/**
 * @author Enaium
 */
class StatusBarItem(project: Project) : EditorBasedStatusBarPopup(project, false) {
    override fun ID(): String {
        return "${JimmerBuddy.NAME}BarItem"
    }

    override fun createInstance(project: Project): StatusBarWidget {
        return StatusBarItem(project)
    }

    override fun createPopup(context: DataContext): ListPopup {
        val initialize = ActionManager.getInstance().getAction("cn.enaium.jimmer.buddy.action.Initialize")
        return JBPopupFactory.getInstance().createActionGroupPopup(
            JimmerBuddy.NAME,
            DefaultActionGroup(listOf(initialize)),
            context,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            false
        )
    }

    override fun getWidgetState(file: VirtualFile?): WidgetState {
        return WidgetState(JimmerBuddy.NAME, null, true).apply {
            icon = JimmerBuddy.Icons.LOGO
        }
    }

    override fun isEnabledForFile(file: VirtualFile?): Boolean {
        return file != null && project.isJimmerProject()
    }
}