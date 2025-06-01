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

package cn.enaium.jimmer.buddy.extensions.insight.template.postfix

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

/**
 * @author Enaium
 */
abstract class AbstractBuddyPostfixTemplateProvider : PostfixTemplateProvider {
    override fun isTerminalSymbol(currentChar: Char): Boolean {
        return currentChar == '.'
    }

    override fun preExpand(file: PsiFile, editor: Editor) {

    }

    override fun afterExpand(file: PsiFile, editor: Editor) {

    }

    override fun preCheck(
        copyFile: PsiFile,
        realEditor: Editor,
        currentOffset: Int
    ): PsiFile {
        return copyFile
    }
}