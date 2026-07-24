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
import com.intellij.psi.util.PsiTreeUtil

/**
 * @author Enaium
 */
class DtoCompletionContributor : CompletionContributor() {

    private val basic = CompletionType.BASIC

    init {
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParent(DtoPsiFile::class.java),
            ExportKeywordCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParents(
                    DtoPsiQualifiedNamePart::class.java,
                    DtoPsiQualifiedName::class.java,
                    DtoPsiExportStatement::class.java
                ),
            ExportTypeCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParents(
                    DtoPsiQualifiedNamePart::class.java,
                    DtoPsiQualifiedName::class.java,
                    DtoPsiPackageStatement::class.java,
                    DtoPsiExportPackage::class.java
                ),
            ExportPackageCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParent(DtoPsiFile::class.java),
            ImportKeywordCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParents(
                    DtoPsiQualifiedNamePart::class.java,
                    DtoPsiQualifiedName::class.java,
                    DtoPsiImportStatement::class.java
                ),
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
                DtoPsiPatterns.psiElement().withParents(
                    DtoPsiPropName::class.java,
                    DtoPsiPositiveProp::class.java,
                    DtoPsiExplicitProp::class.java,
                    DtoPsiDtoBody::class.java
                ),
                DtoPsiPatterns.psiElement().withParents(
                    DtoPsiPropName::class.java,
                    DtoPsiNegativeProp::class.java,
                    DtoPsiExplicitProp::class.java,
                    DtoPsiDtoBody::class.java
                ),
                DtoPsiPatterns.psiElement()
                    .withParents(DtoPsiUserProp::class.java, DtoPsiExplicitProp::class.java, DtoPsiDtoBody::class.java),
                DtoPsiPatterns.psiElement()
                    .withParents(DtoPsiFoldProp::class.java, DtoPsiExplicitProp::class.java, DtoPsiDtoBody::class.java)
            ),
            PropCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParents(
                    DtoPsiQualifiedNamePart::class.java,
                    DtoPsiQualifiedName::class.java,
                    DtoPsiAnnotation::class.java
                ),
            AnnotationQNameCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParents(DtoPsiDirective::class.java, DtoPsiMacro::class.java, DtoPsiDtoBody::class.java),
            MacroNameCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParent(DtoPsiPropPrefix::class.java),
            ConfigNameCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParent(DtoPsiFile::class.java),
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
            DtoPsiPatterns.psiElement()
                .withParents(
                    DtoPsiQualifiedNamePart::class.java,
                    DtoPsiQualifiedName::class.java,
                    DtoPsiDtoType::class.java
                ),
            InterfaceQNameCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParents(
                    DtoPsiQualifiedNamePart::class.java,
                    DtoPsiQualifiedName::class.java,
                    DtoPsiDtoFragment::class.java
                ),
            ForTypeCompletionProvider
        )

        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParent(DtoPsiFile::class.java),
            ImplementsKeywordCompletion
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParents(
                    DtoPsiQualifiedNamePart::class.java,
                    DtoPsiQualifiedName::class.java,
                    DtoPsiTypeRef::class.java,
                    DtoPsiUserProp::class.java
                ),
            TypeRefQNameCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParents(DtoPsiEnumMapping::class.java, DtoPsiEnumBody::class.java),
            EnumEntryCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParents(
                    DtoPsiQualifiedNamePart::class.java,
                    DtoPsiQualifiedName::class.java,
                    DtoPsiScalarMacro::class.java
                ),
            MacroArgCompletionProvider
        )
        extend(
            basic,
            StandardPatterns.or(
                DtoPsiPatterns.psiElement().withParents(
                    DtoPsiQualifiedNamePart::class.java, DtoPsiQualifiedName::class.java,
                    DtoPsiAnnotationValue::class.java, DtoPsiAnnotationArgument::class.java
                ),
                DtoPsiPatterns.psiElement().withParent(DtoPsiAnnotationArgumentsClause::class.java),
                DtoPsiPatterns.psiElement().withParent(DtoPsiAnnotationArguments::class.java),
                DtoPsiPatterns.psiElement().withParents(
                    DtoPsiAnnotationNamedArgument::class.java,
                    DtoPsiAnnotationArgument::class.java,
                    DtoPsiAnnotationArguments::class.java,
                    DtoPsiAnnotationArgumentsClause::class.java
                )
            ),
            AnnotationParametersCompletionProvider
        )
        extend(
            basic,
            DtoPsiPatterns.psiElement()
                .withParents(
                    DtoPsiQualifiedNamePart::class.java,
                    DtoPsiQualifiedName::class.java,
                    DtoPsiTypeBranch::class.java,
                    DtoPsiTypesElement::class.java,
                    DtoPsiTypesBlock::class.java
                ),
            TypesBranchCompletionProvider
        )
    }
}

fun CompletionParameters.getParts(): List<String> {
    val qualifiedName = PsiTreeUtil.getParentOfType(position, DtoPsiQualifiedName::class.java) ?: return emptyList()
    val allParts = qualifiedName.qualifiedNamePartList
    val currentPart = PsiTreeUtil.getParentOfType(position, DtoPsiQualifiedNamePart::class.java)
    val parts = if (currentPart != null) {
        allParts.takeWhile { it != currentPart }
    } else {
        allParts
    }
    return parts.map { it.identifier.text }
}