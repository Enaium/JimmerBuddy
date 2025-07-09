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

import cn.enaium.jimmer.buddy.dto.DtoLexer
import cn.enaium.jimmer.buddy.dto.DtoParser
import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage
import cn.enaium.jimmer.buddy.extensions.dto.DtoLexerAdaptor
import cn.enaium.jimmer.buddy.extensions.dto.DtoParserAdaptor
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiFile
import cn.enaium.jimmer.buddy.extensions.dto.psi.impl.*
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.antlr.intellij.adaptor.lexer.PSIElementTypeFactory
import org.antlr.intellij.adaptor.lexer.RuleIElementType
import org.antlr.intellij.adaptor.lexer.TokenIElementType
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DtoParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer {
        return DtoLexerAdaptor
    }

    override fun createParser(project: Project?): PsiParser {
        return DtoParserAdaptor
    }

    val file = IFileElementType(DtoLanguage)

    override fun getFileNodeType(): IFileElementType {
        return file
    }

    override fun getWhitespaceTokens(): TokenSet {
        return PSIElementTypeFactory.createTokenSet(DtoLanguage, DtoLexer.WhiteSpace)
    }

    override fun getCommentTokens(): TokenSet {
        return PSIElementTypeFactory.createTokenSet(
            DtoLanguage,
            DtoLexer.DocComment,
            DtoLexer.BlockComment,
            DtoLexer.LineComment
        )
    }

    override fun getStringLiteralElements(): TokenSet {
        return PSIElementTypeFactory.createTokenSet(DtoLanguage, DtoLexer.StringLiteral, DtoLexer.SqlStringLiteral)
    }

    override fun createElement(node: ASTNode): PsiElement {
        val elementType = node.elementType
        if (elementType is TokenIElementType) {
            return ANTLRPsiNode(node)
        }
        if (elementType !is RuleIElementType) {
            return ANTLRPsiNode(node)
        }

        return when (elementType.ruleIndex) {
            DtoParser.RULE_dto -> DtoPsiRootImpl(node)
            DtoParser.RULE_exportStatement -> DtoPsiExportStatementImpl(node)
            DtoParser.RULE_typeParts -> DtoPsiTypePartsImpl(node)
            DtoParser.RULE_packageParts -> DtoPsiPackagePartsImpl(node)
            DtoParser.RULE_part -> DtoPsiPartImpl(node)
            DtoParser.RULE_importStatement -> DtoPsiImportStatementImpl(node)
            DtoParser.RULE_importedType -> DtoPsiImportedTypeImpl(node)
            DtoParser.RULE_dtoType -> DtoPsiDtoTypeImpl(node)
            DtoParser.RULE_modifier -> DtoPsiModifierImpl(node)
            DtoParser.RULE_name -> DtoPsiNameImpl(node)
            DtoParser.RULE_implements -> DtoPsiImplementsImpl(node)
            DtoParser.RULE_typeRef -> DtoPsiTypeRefImpl(node)
            DtoParser.RULE_genericArgument -> DtoPsiGenericArgumentImpl(node)
            DtoParser.RULE_dtoBody -> DtoPsiDtoBodyImpl(node)
            DtoParser.RULE_macro -> DtoPsiMacroImpl(node)
            DtoParser.RULE_explicitProp -> DtoPsiExplicitPropImpl(node)
            DtoParser.RULE_aliasGroup -> DtoPsiAliasGroupImpl(node)
            DtoParser.RULE_aliasGroupBody -> DtoPsiAliasGroupBodyImpl(node)
            DtoParser.RULE_aliasPattern -> DtoPsiAliasPatternImpl(node)
            DtoParser.RULE_original -> DtoPsiAliasPatternImpl.Companion.OriginalImpl(node)
            DtoParser.RULE_replacement -> DtoPsiAliasPatternImpl.Companion.ReplacementImpl(node)
            DtoParser.RULE_positiveProp -> DtoPsiPositivePropImpl(node)
            DtoParser.RULE_negativeProp -> DtoPsiNegativePropImpl(node)
            DtoParser.RULE_userProp -> DtoPsiUserPropImpl(node)
            DtoParser.RULE_alias -> DtoPsiAliasImpl(node)
            DtoParser.RULE_prop -> DtoPsiPropImpl(node)
            DtoParser.RULE_annotation -> DtoPsiAnnotationImpl(node)
            DtoParser.RULE_qualifiedName -> DtoPsiQualifiedNameImpl(node)
            DtoParser.RULE_qualifiedNameParts -> DtoPsiQualifiedNamePartsImpl(node)
            DtoParser.RULE_enumBody -> DtoPsiEnumBodyImpl(node)
            DtoParser.RULE_enumMapping -> DtoPsiEnumMappingImpl(node)
            else -> ANTLRPsiNode(node)
        }
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile {
        return DtoPsiFile(viewProvider)
    }
}