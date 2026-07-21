package cn.enaium.jimmer.buddy.extensions.dto

import cn.enaium.jimmer.buddy.extensions.dto.lexer.DtoLexer
import com.intellij.lexer.FlexAdapter

/**
 * @author Enaium
 */
class DtoLexerAdapter : FlexAdapter(DtoLexer(null))