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

package cn.enaium.jimmer.buddy.extensions.dto.lang.highlight

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.dto.DtoLexer
import cn.enaium.jimmer.buddy.extensions.dto.DtoLexerAdaptor
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import org.antlr.intellij.adaptor.lexer.TokenIElementType

class DtoSyntaxHighlighter : SyntaxHighlighterBase() {

    val empty = arrayOf<TextAttributesKey>()
    val identifier = createTextAttributesKey(
        "${JimmerBuddy.DTO_LANGUAGE_ID}.IDENTIFIER",
        DefaultLanguageHighlighterColors.IDENTIFIER
    )
    val keyword = createTextAttributesKey(
        "${JimmerBuddy.DTO_LANGUAGE_ID}.KEYWORD",
        DefaultLanguageHighlighterColors.KEYWORD
    )

    val string = createTextAttributesKey(
        "${JimmerBuddy.DTO_LANGUAGE_ID}.STRING",
        DefaultLanguageHighlighterColors.STRING
    )

    val lineComment = createTextAttributesKey(
        "${JimmerBuddy.DTO_LANGUAGE_ID}.COMMENT",
        DefaultLanguageHighlighterColors.LINE_COMMENT
    )

    val blockComment = createTextAttributesKey(
        "${JimmerBuddy.DTO_LANGUAGE_ID}.BLOCK_COMMENT",
        DefaultLanguageHighlighterColors.BLOCK_COMMENT
    )

    override fun getHighlightingLexer(): Lexer {
        return DtoLexerAdaptor
    }

    override fun getTokenHighlights(tokenType: IElementType?): Array<out TextAttributesKey?> {
        if (tokenType !is TokenIElementType) return arrayOf<TextAttributesKey>()
        return when (tokenType.antlrTokenType) {
            DtoLexer.Identifier -> identifier
            DtoLexer.EXPORT,
            DtoLexer.PACKAGE,
            DtoLexer.IMPORT,
            DtoLexer.AS,
            DtoLexer.FIXED,
            DtoLexer.STATIC,
            DtoLexer.DYNAMIC,
            DtoLexer.FUZZY,
            DtoLexer.IMPLEMENTS,
            DtoLexer.NULL,
            DtoLexer.CONFIG_WHERE,
            DtoLexer.OR,
            DtoLexer.AND,
            DtoLexer.IS,
            DtoLexer.NOT,
            DtoLexer.CONFIG_ORDER_BY,
            DtoLexer.CONFIG_FILTER,
            DtoLexer.CONFIG_RECURSION,
            DtoLexer.CONFIG_FETCH_TYPE,
            DtoLexer.CONFIG_LIMIT,
            DtoLexer.CONFIG_BATCH,
            DtoLexer.CONFIG_DEPTH,
            DtoLexer.TRUE,
            DtoLexer.FALSE -> keyword

            DtoLexer.StringLiteral,
            DtoLexer.SqlStringLiteral -> string

            DtoLexer.LineComment -> lineComment
            DtoLexer.BlockComment, DtoLexer.DocComment -> blockComment
            else -> null
        }?.let { arrayOf(it) } ?: empty
    }
}
