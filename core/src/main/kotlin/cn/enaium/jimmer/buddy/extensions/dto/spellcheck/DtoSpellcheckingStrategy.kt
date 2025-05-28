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

package cn.enaium.jimmer.buddy.extensions.dto.spellcheck

import cn.enaium.jimmer.buddy.dto.DtoParser.RULE_name
import cn.enaium.jimmer.buddy.dto.DtoParser.RULE_prop
import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage
import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage.RULE
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy
import com.intellij.spellchecker.tokenizer.Tokenizer

/**
 * @author Enaium
 */
class DtoSpellcheckingStrategy : SpellcheckingStrategy(), DumbAware {
    override fun isMyContext(element: PsiElement): Boolean {
        return DtoLanguage.`is`(element.language)
    }

    override fun getTokenizer(element: PsiElement?): Tokenizer<*> {
        return when (element?.node?.elementType) {
            RULE[RULE_name], RULE[RULE_prop] -> TEXT_TOKENIZER
            else -> super.getTokenizer(element)
        }
    }
}