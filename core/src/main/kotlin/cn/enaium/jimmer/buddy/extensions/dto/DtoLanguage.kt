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

package cn.enaium.jimmer.buddy.extensions.dto

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.dto.DtoParser
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiElement
import com.intellij.lang.Language
import com.intellij.psi.tree.IElementType
import org.antlr.intellij.adaptor.lexer.PSIElementTypeFactory
import org.antlr.intellij.adaptor.lexer.RuleIElementType
import org.antlr.intellij.adaptor.xpath.XPath

object DtoLanguage : Language(JimmerBuddy.DTO_LANGUAGE_ID) {
    private fun readResolve(): Any = DtoLanguage
    val XPATH: XPath
        get() = XPath(DtoLanguage, "")

    val RULE: List<RuleIElementType>
        get() = PSIElementTypeFactory.getRuleIElementTypes(DtoLanguage)

    val TOKEN: List<IElementType>
        get() = PSIElementTypeFactory.getTokenIElementTypes(DtoLanguage)

    init {
        val vocab = DtoParser.VOCABULARY
        PSIElementTypeFactory.defineLanguageIElementTypes(
            DtoLanguage,
            Array(vocab.maxTokenType + 1) { vocab.getDisplayName(it) },
            DtoParser.ruleNames,
        )
    }

    inline fun <reified T : DtoPsiElement> DtoPsiElement.findChild(path: String): T? {
        return this.findChildren<T>(path).firstOrNull()
    }

    inline fun <reified T : DtoPsiElement> DtoPsiElement.findChildren(path: String): List<T> {
        return XPATH.evaluate(this, XPATH.split(path)).map { it as T }
    }
}