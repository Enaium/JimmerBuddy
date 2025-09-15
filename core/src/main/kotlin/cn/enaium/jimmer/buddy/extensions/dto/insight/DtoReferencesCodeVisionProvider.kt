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

package cn.enaium.jimmer.buddy.extensions.dto.insight

import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoType
import cn.enaium.jimmer.buddy.utility.generatedName
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import com.intellij.codeInsight.hints.codeVision.CodeVisionProviderBase
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
import com.intellij.java.JavaBundle
import com.intellij.openapi.editor.Editor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.ui.awt.RelativePoint
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.base.util.projectScope
import java.awt.event.MouseEvent

/**
 * @author Enaium
 */
class DtoReferencesCodeVisionProvider : CodeVisionProviderBase() {
    override val name: String
        get() = JavaBundle.message("settings.inlay.java.usages")
    override val relativeOrderings: List<CodeVisionRelativeOrdering>
        get() = listOf(CodeVisionRelativeOrdering.CodeVisionRelativeOrderingBefore("vcs.code.vision"))
    override val id: String
        get() = "JimmerBuddy.dto.usages"

    override fun acceptsElement(element: PsiElement): Boolean {
        return element is DtoPsiDtoType
    }

    override fun acceptsFile(file: PsiFile): Boolean {
        return file.language == DtoLanguage
    }

    override fun getHint(element: PsiElement, file: PsiFile): String? {
        if (element is DtoPsiDtoType) {
            val target =
                JavaPsiFacade.getInstance(element.project)
                    .findClass(element.generatedName() ?: return null, element.project.allScope())
                    ?: return null

            val search = ReferencesSearch.search(target, element.project.projectScope())
            return JavaBundle.message("usages.telescope", search.count())
        }
        return null
    }

    override fun handleClick(
        editor: Editor,
        element: PsiElement,
        event: MouseEvent?
    ) {
        if (element is DtoPsiDtoType) {
            val target =
                JavaPsiFacade.getInstance(element.project)
                    .findClass(element.generatedName() ?: return, element.project.allScope())
                    ?: return

            GotoDeclarationAction.startFindUsages(
                editor,
                target.project,
                target,
                if (event == null) null else RelativePoint(event)
            )
        }
    }
}