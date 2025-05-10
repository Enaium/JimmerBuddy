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

package cn.enaium.jimmer.buddy.extensions.dto.lang

import cn.enaium.jimmer.buddy.dto.DtoParser.*
import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage.TOKEN
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

/**
 * @author Enaium
 */
class DtoPairedBraceMatcher : PairedBraceMatcher {
    override fun getPairs(): Array<BracePair> {
        return arrayOf(
            BracePair(TOKEN[LEFT_PARENTHESIS], TOKEN[RIGHT_PARENTHESIS], true),
            BracePair(TOKEN[LEFT_BRACE], TOKEN[RIGHT_BRACE], true),
            BracePair(TOKEN[LEFT_BRACKET], TOKEN[RIGHT_BRACKET], true),
        )
    }

    override fun isPairedBracesAllowedBeforeType(
        p0: IElementType,
        p1: IElementType?
    ): Boolean {
        return true
    }

    override fun getCodeConstructStart(file: PsiFile, offset: Int): Int {

        val element = file.findElementAt(offset)
        if (element == null || element is PsiFile) {
            return offset
        }

        return offset
    }
}