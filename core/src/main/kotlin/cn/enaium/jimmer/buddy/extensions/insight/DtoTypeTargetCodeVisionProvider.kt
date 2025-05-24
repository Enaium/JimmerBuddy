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

package cn.enaium.jimmer.buddy.extensions.insight

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoType
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiRoot
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.FilenameIndex
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElementOfType
import java.awt.event.MouseEvent

/**
 * @author Enaium
 */
class DtoTypeTargetCodeVisionProvider : ImmutableCodeVisionProvider() {

    override val id: String
        get() = "JimmerBuddy.dto.target"
    override val name: String
        get() = "DTO Target"

    override fun getHint(element: PsiElement, file: PsiFile): String? {
        val project = element.project
        val qualifiedName = element.toUElementOfType<UClass>()?.qualifiedName ?: return null
        val count = findDtoTypes(project, qualifiedName).size
        return if (count > 0) {
            "$count DTO Types"
        } else {
            null
        }
    }

    override fun handleClick(
        editor: Editor,
        element: PsiElement,
        event: MouseEvent?
    ) {
        val project = editor.project ?: return
        val qualifiedName = element.toUElementOfType<UClass>()?.qualifiedName ?: return
        JimmerBuddy.Services.NAVIGATION.getPsiElementPopup(
            findDtoTypes(project, qualifiedName),
            "Choose DTO Type"
        ).showInBestPositionFor(editor)
    }

    private fun findDtoTypes(
        project: Project,
        qualifiedName: String
    ): List<DtoPsiDtoType> = FilenameIndex.getAllFilesByExt(project, "dto")
        .mapNotNull { it.toPsiFile(project)?.getChildOfType<DtoPsiRoot>() }
        .find { it.exportStatement?.typeParts?.qualifiedName == qualifiedName }?.dtoTypes ?: emptyList()
}