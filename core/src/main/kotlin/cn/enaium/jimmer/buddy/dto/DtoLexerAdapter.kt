package cn.enaium.jimmer.buddy.dto

import cn.enaium.jimmer.buddy.dto.lexer.DtoLexer
import com.intellij.lexer.FlexAdapter

/**
 * @author Enaium
 */
class DtoLexerAdapter : FlexAdapter(DtoLexer(null))