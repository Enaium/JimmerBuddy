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

package cn.enaium.jimmer.buddy.extensions.dto.completion

import cn.enaium.jimmer.buddy.extensions.dto.pattern.DtoPsiPatterns
import cn.enaium.jimmer.buddy.extensions.dto.psi.*
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.psi.util.siblings

/**
 * @author Enaium
 */
class DtoCompletionContributor : CompletionContributor() {

    private val basic = CompletionType.BASIC

    init {
        extend(
            basic,
            DtoPsiPatterns.psiElement(),
            ExportKeywordCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParent(DtoPsiExportStatement::class.java),
            ExportTypeCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParent(DtoPsiExportStatement::class.java),
            ExportPackageCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement(),
            ImportKeywordCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParent(DtoPsiImportStatement::class.java),
            ImportPartsCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParent(DtoPsiImportedType::class.java),
            ImportedTypeCompletionProvider
        )
        extend(
            basic,
            StandardPatterns.or(
                DtoPsiPatterns.psiElement().withParent(DtoPsiPositiveProp::class.java)
                    .inside(DtoPsiDtoBody::class.java),
                DtoPsiPatterns.psiElement().withParent(DtoPsiNegativeProp::class.java)
                    .inside(DtoPsiDtoBody::class.java),
                DtoPsiPatterns.psiElement().withParent(DtoPsiUserProp::class.java)
                    .inside(DtoPsiDtoBody::class.java),
                DtoPsiPatterns.psiElement().withParent(DtoPsiFoldProp::class.java)
                    .inside(DtoPsiDtoBody::class.java)
            ),
            PropCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParents(
                    DtoPsiQualifiedName::class.java,
                    DtoPsiAnnotation::class.java
                ),
            AnnotationQNameCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParent(DtoPsiMacro::class.java)
                .inside(DtoPsiDtoBody::class.java),
            MacroNameCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .inside(DtoPsiDtoBody::class.java),
            ConfigNameCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement(),
            ModifierCompletionProvider
        )
        extend(
            basic,
            StandardPatterns.or(
                DtoPsiPatterns.psiElement().withParent(DtoPsiPositiveProp::class.java)
                    .inside(DtoPsiDtoBody::class.java),
                DtoPsiPatterns.psiElement().withParent(DtoPsiNegativeProp::class.java)
                    .inside(DtoPsiDtoBody::class.java),
                DtoPsiPatterns.psiElement().withParent(DtoPsiUserProp::class.java)
                    .inside(DtoPsiDtoBody::class.java),
                DtoPsiPatterns.psiElement().withParent(DtoPsiFoldProp::class.java)
                    .inside(DtoPsiDtoBody::class.java)
            ),
            FuncNameCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement().withParents(
                DtoPsiQualifiedName::class.java,
                DtoPsiTypeRef::class.java,
                DtoPsiDtoType::class.java
            ),
            InterfaceQNameCompletionProvider
        )

        extend(
            basic,
            StandardPatterns.or(
                DtoPsiPatterns.psiElement().withParent(DtoPsiPositiveProp::class.java)
                    .afterLeaf(DtoPsiPatterns.psiElement().withParent(DtoPsiPositiveProp::class.java)),
                DtoPsiPatterns.psiElement().inside(DtoPsiDtoType::class.java)
                    .afterLeaf(DtoPsiPatterns.psiElement())
            ),
            ImplementsKeywordCompletion
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement().withParents(
                DtoPsiQualifiedName::class.java,
                DtoPsiTypeRef::class.java,
                DtoPsiUserProp::class.java
            ),
            TypeRefQNameCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParent(DtoPsiEnumMapping::class.java)
                .inside(DtoPsiEnumBody::class.java),
            EnumEntryCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement().withParents(
                DtoPsiQualifiedName::class.java,
                DtoPsiMacro::class.java
            ),
            MacroArgCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement().withParents(
                DtoPsiQualifiedName::class.java,
                DtoPsiIncludeMacro::class.java
            ),
            IncludeCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement().withParents(
                DtoPsiAnnotationNamedArgument::class.java,
            ).inside(DtoPsiAnnotationArguments::class.java),
            AnnotationParametersCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement().withParents(
                DtoPsiQualifiedName::class.java,
                DtoPsiTypeBranch::class.java
            ).inside(DtoPsiTypesBlock::class.java),
            TypesBranchCompletionProvider
        )
    }
}

fun CompletionParameters.getParts(): List<String> {
    return this.position.parent?.siblings(forward = false, withSelf = false)
        ?.filter { it.elementType == DtoTypes.IDENTIFIER }
        ?.map(PsiElement::getText)
        ?.toList()
        ?.asReversed() ?: emptyList()
}