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

import cn.enaium.jimmer.buddy.dto.DtoParser
import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage
import cn.enaium.jimmer.buddy.extensions.dto.pattern.DtoPsiPatterns
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiAnnotation
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoBody
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoType
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiExportStatement
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiImplements
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiQualifiedNameParts
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiImportStatement
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiImportedType
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiMacro
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiName
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiPackageParts
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiPart
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiProp
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiQualifiedName
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiRoot
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiTypeParts
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiTypeRef
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiUserProp
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
            DtoPsiPatterns.psiElement().withSuperParent(3, DtoPsiRoot::class.java),
            ExportKeywordCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParents(
                    DtoPsiPart::class.java,
                    DtoPsiTypeParts::class.java,
                    DtoPsiExportStatement::class.java
                ),
            ExportTypeCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParents(
                    DtoPsiPart::class.java,
                    DtoPsiPackageParts::class.java,
                    DtoPsiExportStatement::class.java
                ),
            ExportPackageCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement().withSuperParent(3, DtoPsiRoot::class.java),
            ImportKeywordCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParents(
                    DtoPsiPart::class.java,
                    DtoPsiQualifiedNameParts::class.java,
                    DtoPsiImportStatement::class.java
                ),
            ImportPartsCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParents(
                    DtoPsiName::class.java,
                    DtoPsiImportedType::class.java,
                    DtoPsiImportStatement::class.java
                ),
            ImportedTypeCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParent(DtoPsiProp::class.java)
                .inside(DtoPsiDtoBody::class.java),
            PropCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParents(
                    DtoPsiPart::class.java,
                    DtoPsiQualifiedNameParts::class.java,
                    DtoPsiQualifiedName::class.java,
                    DtoPsiAnnotation::class.java
                ),
            AnnotationQNameCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParents(DtoPsiName::class.java, DtoPsiMacro::class.java)
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
            DtoPsiPatterns.psiElement().withSuperParent(3, DtoPsiRoot::class.java),
            ModifierCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .inside(DtoPsiDtoBody::class.java),
            FuncNameCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement().withParents(
                DtoPsiPart::class.java,
                DtoPsiQualifiedNameParts::class.java,
                DtoPsiQualifiedName::class.java,
                DtoPsiTypeRef::class.java,
                DtoPsiImplements::class.java
            ).inside(DtoPsiDtoType::class.java),
            InterfaceQNameCompletionProvider
        )

        extend(
            basic,
            StandardPatterns.or(
                DtoPsiPatterns.psiElement().withParent(DtoPsiProp::class.java)
                    .afterLeaf(DtoPsiPatterns.psiElement().withParent(DtoPsiProp::class.java)),
                DtoPsiPatterns.psiElement().withSuperParent(2, DtoPsiDtoType::class.java)
                    .afterLeaf(DtoPsiPatterns.psiElement().withParent(DtoPsiName::class.java))
            ),
            ImplementsKeywordCompletion
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement().withParents(
                DtoPsiPart::class.java,
                DtoPsiQualifiedNameParts::class.java,
                DtoPsiQualifiedName::class.java,
                DtoPsiTypeRef::class.java
            ).inside(DtoPsiUserProp::class.java),
            TypeRefQNameCompletionProvider
        )
    }
}

fun CompletionParameters.getParts(): List<String> {
    return this.position.parent?.siblings(forward = false, withSelf = false)
        ?.filter { it.elementType == DtoLanguage.RULE[DtoParser.RULE_part] }
        ?.map(PsiElement::getText)
        ?.toList()
        ?.asReversed() ?: emptyList()
}