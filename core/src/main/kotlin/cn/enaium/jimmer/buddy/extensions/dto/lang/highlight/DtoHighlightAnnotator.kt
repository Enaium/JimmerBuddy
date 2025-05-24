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
import cn.enaium.jimmer.buddy.dto.DtoParser
import cn.enaium.jimmer.buddy.dto.DtoParser.AT
import cn.enaium.jimmer.buddy.dto.DtoParser.HASH
import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage
import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage.TOKEN
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiAlias
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiAnnotation
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiMacro
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiModifier
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiName
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiPart
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiProp
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiQualifiedName
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.elementType
import com.intellij.psi.util.findParentOfType

class DtoHighlightAnnotator : Annotator {

    val keyword = createTextAttributesKey(
        "${JimmerBuddy.DTO_LANGUAGE_ID}.KEYWORD",
        DefaultLanguageHighlighterColors.KEYWORD
    )

    val variable = createTextAttributesKey(
        "${JimmerBuddy.DTO_LANGUAGE_ID}.VARIABLE",
        DefaultLanguageHighlighterColors.LOCAL_VARIABLE
    )

    val property = createTextAttributesKey(
        "${JimmerBuddy.DTO_LANGUAGE_ID}.PROPERTY",
        DefaultLanguageHighlighterColors.INSTANCE_FIELD
    )

    val macro = createTextAttributesKey(
        "${JimmerBuddy.DTO_LANGUAGE_ID}.MACRO",
        DefaultLanguageHighlighterColors.KEYWORD
    )

    val annotation = createTextAttributesKey(
        "${JimmerBuddy.DTO_LANGUAGE_ID}.ANNOTATION",
        DefaultLanguageHighlighterColors.METADATA
    )

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val elementType = element.elementType
        when (element) {
            is DtoPsiModifier -> keyword
            is DtoPsiProp -> property
            is DtoPsiAlias -> variable
            is DtoPsiName -> element.findParentOfType<DtoPsiMacro>()?.let { macro }
            is DtoPsiPart -> element.findParentOfType<DtoPsiAnnotation>()?.let { annotation }
            else -> when (elementType) {
                TOKEN[HASH] -> {
                    element.findParentOfType<DtoPsiMacro>()?.let { macro }
                }

                TOKEN[AT] -> {
                    element.findParentOfType<DtoPsiAnnotation>()?.let { annotation }
                }

                else -> {
                    null
                }
            }
        }?.also {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION).textAttributes(it).create()
        }
    }
}