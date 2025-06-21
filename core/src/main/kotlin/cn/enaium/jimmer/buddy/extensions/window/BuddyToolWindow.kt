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

import cn.enaium.jimmer.buddy.extensions.window.panel.DTOList
import cn.enaium.jimmer.buddy.extensions.window.panel.DatabaseList
import cn.enaium.jimmer.buddy.extensions.window.panel.ErrorFamilyList
import cn.enaium.jimmer.buddy.extensions.window.panel.ImmutableTree
import cn.enaium.jimmer.buddy.utility.isJimmerProject
import cn.enaium.jimmer.buddy.utility.I18n
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

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
                ImmutableTree(project),
                I18n.message("toolWindow.buddy.tab.immutables"),
                false
            )
        )
        toolWindow.contentManager.addContent(
            toolWindow.contentManager.factory.createContent(
                DTOList(project),
                I18n.message("toolWindow.buddy.tab.dto"),
                false
            )
        )
        toolWindow.contentManager.addContent(
            toolWindow.contentManager.factory.createContent(
                ErrorFamilyList(project),
                I18n.message("toolWindow.buddy.tab.errorFamilies"),
                false
            )
        )
        toolWindow.contentManager.addContent(
            toolWindow.contentManager.factory.createContent(
                DatabaseList(project),
                I18n.message("toolWindow.buddy.tab.Databases"),
                false
            )
        )
    }

    override fun shouldBeAvailable(project: Project): Boolean {
        return project.isJimmerProject()
    }
}