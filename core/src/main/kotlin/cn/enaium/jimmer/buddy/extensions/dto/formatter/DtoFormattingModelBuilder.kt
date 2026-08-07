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

package cn.enaium.jimmer.buddy.extensions.dto.formatter

import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoTypes.*
import cn.enaium.jimmer.buddy.utility.around
import cn.enaium.jimmer.buddy.utility.emptyLine
import com.intellij.formatting.*

/**
 * @author Enaium
 */
class DtoFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val styleSettings = formattingContext.codeStyleSettings
        val spacingBuilder = SpacingBuilder(styleSettings, DtoLanguage)
            .around(DOT).spaces(0)
            .around(COMMA, 0, 1)
            .around(COLON, 0, 1)
            .around(SEMI, 0, 1)
            .around(ARROW).spaces(1)
            .around(EQ).spaces(1)
            .between(AS, LPAREN).spaces(0)
            .around(AS).spaces(1)
            .around(INPUT).spaces(1)
            .around(SPECIFICATION).spaces(1)
            .around(UNSAFE).spaces(1)
            .around(FIXED).spaces(1)
            .around(STATIC).spaces(1)
            .around(DYNAMIC).spaces(1)
            .around(FUZZY).spaces(1)
            .around(SEALED).spaces(1)
            .around(IMPLEMENTS).spaces(1)
            .around(LPAREN).spaces(0)
            .before(RPAREN).spaces(0)
            .after(AT).spaces(0)
            .after(EXPORT_STATEMENT).emptyLine(1)
            .before(EXPORT).spaceIf(false)
            .between(EXPORT, IDENTIFIER).spaces(1)
            .between(ARROW, PACKAGE).spaces(1)
            .between(PACKAGE, IDENTIFIER).spaces(1)
            .before(IMPORT_STATEMENT).spaceIf(false)
            .between(IMPORT, IDENTIFIER).spaces(1)
            .between(LBRACE, IMPORTED_TYPE).spaces(1)
            .between(IMPORTED_TYPE, RBRACE).spaces(1)
            .before(DTO_TYPE).spaceIf(false)
            .between(IDENTIFIER, DTO_BODY).spaces(1)
            .between(RPAREN, DTO_BODY).spaces(1)
            .between(IMPLEMENTS, TYPE_REF).spaces(1)
        return FormattingModelProvider.createFormattingModelForPsiFile(
            formattingContext.containingFile,
            DtoBlock(
                formattingContext.node,
                Wrap.createWrap(WrapType.NONE, false),
                null,
                spacingBuilder
            ),
            styleSettings
        )
    }
}