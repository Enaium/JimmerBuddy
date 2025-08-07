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

package cn.enaium.jimmer.buddy.extensions.dto.manipulator

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiProp
import cn.enaium.jimmer.buddy.utility.createDtoProp
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import com.intellij.psi.PsiElement


/**
 * @author Enaium
 */
class DtoPsiPropManipulator : AbstractElementManipulator<DtoPsiProp>() {
    override fun handleContentChange(
        element: DtoPsiProp,
        range: TextRange,
        newContent: String
    ): DtoPsiProp? {
        val oldText = element.text
        val newText = oldText.take(range.startOffset) + newContent + oldText.substring(range.endOffset)
        val newElement: PsiElement? = element.project.createDtoProp(newText)
        if (newElement != null) {
            element.replace(newElement)
            return newElement as DtoPsiProp
        }
        return null
    }

    override fun getRangeInElement(element: DtoPsiProp): TextRange {
        return TextRange(0, element.textLength)
    }
}