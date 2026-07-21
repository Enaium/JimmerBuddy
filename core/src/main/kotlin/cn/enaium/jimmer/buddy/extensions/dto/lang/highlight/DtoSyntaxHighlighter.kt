package cn.enaium.jimmer.buddy.extensions.dto.lang.highlight

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.extensions.dto.BLOCK_COMMENT
import cn.enaium.jimmer.buddy.extensions.dto.DtoLexerAdapter
import cn.enaium.jimmer.buddy.extensions.dto.LINE_COMMENT
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoTypes
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.psi.tree.IElementType

/**
 * @author Enaium
 */
class DtoSyntaxHighlighter : SyntaxHighlighter {

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

    val number = createTextAttributesKey(
        "${JimmerBuddy.DTO_LANGUAGE_ID}.NUMBER",
        DefaultLanguageHighlighterColors.NUMBER
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
        return DtoLexerAdapter()
    }

    override fun getTokenHighlights(tokenType: IElementType): Array<out TextAttributesKey> {
        return when (tokenType) {
            DtoTypes.IDENTIFIER -> identifier
            DtoTypes.EXPORT,
            DtoTypes.PACKAGE,
            DtoTypes.IMPORT,
            DtoTypes.AS,
            DtoTypes.FIXED,
            DtoTypes.STATIC,
            DtoTypes.DYNAMIC,
            DtoTypes.FUZZY,
            DtoTypes.IMPLEMENTS,
            DtoTypes.NULL,
            DtoTypes.CONFIG_WHERE,
            DtoTypes.OR,
            DtoTypes.AND,
            DtoTypes.IS,
            DtoTypes.NOT,
            DtoTypes.CONFIG_ORDER_BY,
            DtoTypes.CONFIG_FILTER,
            DtoTypes.CONFIG_RECURSION,
            DtoTypes.CONFIG_FETCH_TYPE,
            DtoTypes.CONFIG_LIMIT,
            DtoTypes.CONFIG_BATCH,
            DtoTypes.CONFIG_DEPTH,
            DtoTypes.TRUE,
            DtoTypes.FRAGMENT,
            DtoTypes.FOR,
            DtoTypes.FALSE -> keyword

            DtoTypes.STRING_LITERAL,
            DtoTypes.SQL_STRING_LITERAL,
            DtoTypes.CHARACTER_LITERAL -> string

            DtoTypes.INTEGER_LITERAL,
            DtoTypes.FLOATING_POINT_LITERAL -> number

            LINE_COMMENT -> lineComment
            BLOCK_COMMENT, DtoTypes.DOC_COMMENT -> blockComment
            else -> null
        }?.let { arrayOf(it) } ?: emptyArray()
    }
}