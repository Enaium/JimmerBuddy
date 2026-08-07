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

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiAliasGroup
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoBody
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiEnumBody
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor

/**
 * @author Enaium
 */
class DtoFoldingBuilder : FoldingBuilderEx(), DumbAware {
    override fun buildFoldRegions(
        root: PsiElement,
        document: Document,
        quick: Boolean
    ): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()
        root.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                when (element) {
                    is DtoPsiDtoBody, is DtoPsiAliasGroup, is DtoPsiEnumBody -> {
                        descriptors.add(FoldingDescriptor(element, element.textRange))
                    }
                }
                super.visitElement(element)
            }
        })
        return descriptors.toTypedArray()
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        return false
    }

    override fun getPlaceholderText(node: ASTNode): String {
        return "{...}"
    }
}