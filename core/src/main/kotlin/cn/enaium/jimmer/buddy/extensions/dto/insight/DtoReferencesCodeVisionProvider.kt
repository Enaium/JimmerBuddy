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

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoType
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiName
import cn.enaium.jimmer.buddy.utility.generatedReferences
import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.codeInsight.hints.codeVision.CodeVisionProviderBase
import com.intellij.java.JavaBundle
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.PsiTreeUtil
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
        return element is DtoPsiName && element.parent is DtoPsiDtoType
    }

    override fun acceptsFile(file: PsiFile): Boolean {
        return file.language == DtoLanguage
    }

    override fun computeForEditor(
        editor: Editor,
        file: PsiFile
    ): List<Pair<TextRange, CodeVisionEntry>> {
        if (!acceptsFile(file)) {
            return emptyList()
        }

        return PsiTreeUtil.collectElementsOfType(file, DtoPsiDtoType::class.java)
            .mapNotNull { dtoType ->
                val name = dtoType.name ?: return@mapNotNull null
                val hint = getHint(dtoType, file) ?: return@mapNotNull null
                val pointer = SmartPointerManager.createPointer(dtoType)
                val clickHandler = { event: MouseEvent?, clickEditor: Editor ->
                    val element = pointer.element
                    if (element != null) {
                        handleClick(clickEditor, element, event)
                    }
                }
                val entry = ClickableTextCodeVisionEntry(
                    hint,
                    id,
                    clickHandler,
                    null,
                    "",
                    "",
                    emptyList()
                )
                name.textRange to entry
            }
    }

    override fun getHint(element: PsiElement, file: PsiFile): String? {
        val dtoType = element.dtoType() ?: return null
        val count = dtoType.generatedReferences().size
        if (count == 0) {
            return null
        }
        return JavaBundle.message("usages.telescope", count)
    }

    override fun handleClick(
        editor: Editor,
        element: PsiElement,
        event: MouseEvent?
    ) {
        val dtoType = element.dtoType() ?: return
        val references = dtoType.generatedReferences()
        if (references.isEmpty()) {
            return
        }

        JimmerBuddy.Services.NAVIGATION.getPsiElementPopup(references, "Choose DTO Reference")
            .showInBestPositionFor(editor)
    }

    private fun PsiElement.dtoType(): DtoPsiDtoType? {
        return when (this) {
            is DtoPsiDtoType -> this
            is DtoPsiName -> parent as? DtoPsiDtoType
            else -> null
        }
    }
}
