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
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiFile
import com.intellij.psi.search.scope.packageSet.CustomScopesProvider
import com.intellij.psi.search.scope.packageSet.NamedScope
import com.intellij.psi.search.scope.packageSet.NamedScopesHolder
import com.intellij.psi.search.scope.packageSet.PackageSet

/**
 * @author Enaium
 */
class GeneratedSourceScopeProvider : CustomScopesProvider, DumbAware {
    override fun getCustomScopes(): List<NamedScope> {
        return listOf(
            NamedScope(
                I18n.message("scope.nonGeneratedProject"),
                NonGeneratedProjectPackageSet
            )
        )
    }
}

private object NonGeneratedProjectPackageSet : PackageSet {
    override fun contains(file: PsiFile, holder: NamedScopesHolder): Boolean {
        val virtualFile = file.virtualFile ?: file.originalFile.virtualFile ?: return false
        if (isJimmerGeneratedFile(virtualFile.path)) {
            return false
        }
        val fileIndex = ProjectFileIndex.getInstance(holder.project)
        return fileIndex.isInProject(virtualFile)
    }

    override fun createCopy(): PackageSet {
        return this
    }

    override fun getText(): String {
        return "project without generated jimmer artifacts"
    }

    override fun getNodePriority(): Int {
        return 0
    }
}
