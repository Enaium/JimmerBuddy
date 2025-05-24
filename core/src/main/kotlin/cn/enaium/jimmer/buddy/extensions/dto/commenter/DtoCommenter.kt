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

package cn.enaium.jimmer.buddy.extensions.dto.commenter

import cn.enaium.jimmer.buddy.dto.DtoParser
import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage
import com.intellij.lang.CodeDocumentationAwareCommenter
import com.intellij.psi.PsiComment
import com.intellij.psi.tree.IElementType

/**
 * @author Enaium
 */
class DtoCommenter : CodeDocumentationAwareCommenter {
    override fun getLineCommentTokenType(): IElementType {
        return DtoLanguage.TOKEN[DtoParser.LineComment]
    }

    override fun getBlockCommentTokenType(): IElementType {
        return DtoLanguage.TOKEN[DtoParser.BlockComment]
    }

    override fun getDocumentationCommentTokenType(): IElementType {
        return DtoLanguage.TOKEN[DtoParser.DocComment]
    }

    override fun getDocumentationCommentPrefix(): String? {
        return "/**"
    }

    override fun getDocumentationCommentLinePrefix(): String? {
        return "*"
    }

    override fun getDocumentationCommentSuffix(): String? {
        return "*/"
    }

    override fun isDocumentationComment(element: PsiComment): Boolean {
        return element.tokenType == DtoLanguage.TOKEN[DtoParser.DocComment]
    }

    override fun getLineCommentPrefix(): String {
        return "//"
    }

    override fun getBlockCommentPrefix(): String {
        return "/*"
    }

    override fun getBlockCommentSuffix(): String {
        return "*/"
    }

    override fun getCommentedBlockCommentPrefix(): String? {
        return null
    }

    override fun getCommentedBlockCommentSuffix(): String? {
        return null
    }

}