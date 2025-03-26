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

package cn.enaium.jimmer.buddy.extensions.intention

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor

/**
 * @author Enaium
 */
abstract class DraftSetIntentionAction : PsiElementBaseIntentionAction() {

    companion object {
        val caches = mutableMapOf<Int, List<String>>()
    }

    override fun getText(): String {
        return "Generate all set of the draft"
    }

    override fun getFamilyName(): String {
        return "Jimmer"
    }

    fun Editor.insertLines(lines: List<String>) {
        val caretOffset = caretModel.offset
        val caretLine = caretModel.logicalPosition.line
        val lineStartOffset = document.getLineStartOffset(caretLine)
        val lineText = document.text.substring(lineStartOffset, caretOffset)
        val indentation = lineText.takeWhile { it.isWhitespace() }
        val indentedResults = lines.joinToString("\n") { if (it == lines.first()) it else "$indentation$it" }
        if (caches.containsKey(caretOffset)) {
            caches.remove(caretOffset)
        } else {
            caches[caretOffset] = lines
        }
        document.insertString(caretOffset, indentedResults)
    }
}