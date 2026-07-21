package cn.enaium.jimmer.buddy.dto

import cn.enaium.jimmer.buddy.dto.parser.DtoParser
import cn.enaium.jimmer.buddy.dto.psi.DtoTokenType
import cn.enaium.jimmer.buddy.dto.psi.DtoTypes
import cn.enaium.jimmer.buddy.dto.stubs.DtoFileElementType
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiFile
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.IStubFileElementType
import com.intellij.psi.tree.TokenSet

/**
 * @author Enaium
 */
class DtoParserDefinition : ParserDefinition {

    override fun createLexer(project: Project?): Lexer {
        return DtoLexerAdapter()
    }

    override fun createParser(project: Project?): PsiParser {
        return DtoParser()
    }

    override fun getFileNodeType(): IFileElementType {
        return FILE
    }

    override fun getCommentTokens(): TokenSet {
        return COMMENTS
    }

    override fun getStringLiteralElements(): TokenSet {
        return STRINGS;
    }

    override fun createElement(noode: ASTNode): PsiElement {
        return DtoTypes.Factory.createElement(noode);
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile {
        return DtoPsiFile(viewProvider)
    }
}

val FILE: IStubFileElementType<*> = DtoFileElementType()

val BLOCK_COMMENT = DtoTokenType("BLOCK_COMMENT")
val LINE_COMMENT = DtoTokenType("LINE_COMMENT")
val COMMENTS = TokenSet.create(DtoTypes.DOC_COMMENT, BLOCK_COMMENT, LINE_COMMENT)

val STRINGS = TokenSet.create(DtoTypes.STRING_LITERAL, DtoTypes.SQL_STRING_LITERAL)