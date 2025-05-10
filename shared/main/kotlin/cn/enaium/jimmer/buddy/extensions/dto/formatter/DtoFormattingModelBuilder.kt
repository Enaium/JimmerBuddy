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

import cn.enaium.jimmer.buddy.dto.DtoParser.*
import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage
import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage.RULE
import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage.TOKEN
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
            .around(TOKEN[DOT]).spaces(0)//[.]
            .around(TOKEN[COMMA], 0, 1)//[, ]
            .around(TOKEN[COLON], 0, 1)//[: ]
            .around(TOKEN[SEMICOLON], 0, 1)//[; ]
            .around(TOKEN[RIGHT_ARROR]).spaces(1)//[ -> ]
            .around(TOKEN[EQUAL]).spaces(1)//[ = ]
            .between(TOKEN[AS], TOKEN[LEFT_PARENTHESIS]).spaces(0)//[as(]
            .around(TOKEN[AS]).spaces(1)//[ as ]
            .around(RULE[RULE_modifier]).spaces(1)//[ modifier ]
            .after(TOKEN[AT]).spaces(0)//[@]
            .after(TOKEN[HASH]).spaces(0)//[#]
            .after(RULE[RULE_exportStatement]).emptyLine(1)//[export \n]
            .before(TOKEN[EXPORT]).spaceIf(false)//[export]
            .between(TOKEN[EXPORT], RULE[RULE_typeParts]).spaces(1)//[export a.b.c]
            .between(TOKEN[RIGHT_ARROR], TOKEN[PACKAGE]).spaces(1)//[-> package]
            .between(TOKEN[PACKAGE], RULE[RULE_qualifiedNameParts]).spaces(1)//[package a.b.c]
            .before(RULE[RULE_importStatement]).spaceIf(false)//[import]
            .between(TOKEN[IMPORT], RULE[RULE_qualifiedNameParts])
            .spaces(1)//[import a.b.c]
            .before(RULE[RULE_dtoType]).spaceIf(false)//[dtoType]

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