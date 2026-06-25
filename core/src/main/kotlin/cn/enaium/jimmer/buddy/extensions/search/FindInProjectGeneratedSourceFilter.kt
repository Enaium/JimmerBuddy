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

package cn.enaium.jimmer.buddy.extensions.search

import cn.enaium.jimmer.buddy.utility.I18n
import cn.enaium.jimmer.buddy.utility.isJimmerGeneratedFile
import com.intellij.find.FindModel
import com.intellij.find.impl.FindInProjectExtension
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope

/**
 * @author Enaium
 */
class FindInProjectGeneratedSourceFilter : FindInProjectExtension, DumbAware {
    override fun initModelFromContext(model: FindModel, dataContext: DataContext): Boolean {
        val project = CommonDataKeys.PROJECT.getData(dataContext) ?: return false
        if (model.isCustomScope || model.directoryName != null || model.moduleName != null) {
            return false
        }

        model.setProjectScope(false)
        model.setCustomScope(true)
        model.setCustomScope(JimmerGeneratedFreeProjectSearchScope(project))
        model.setCustomScopeName(I18n.message("scope.nonGeneratedProject"))
        return true
    }
}

private class JimmerGeneratedFreeProjectSearchScope(
    private val currentProject: Project
) : GlobalSearchScope(currentProject) {
    override fun contains(file: VirtualFile): Boolean {
        if (isJimmerGeneratedFile(file.path)) {
            return false
        }

        val fileIndex = ProjectFileIndex.getInstance(currentProject)
        return fileIndex.isInProject(file)
    }

    override fun isSearchInModuleContent(aModule: Module): Boolean {
        return true
    }

    override fun isSearchInLibraries(): Boolean {
        return false
    }

    override fun getDisplayName(): String {
        return I18n.message("scope.nonGeneratedProject")
    }
}
