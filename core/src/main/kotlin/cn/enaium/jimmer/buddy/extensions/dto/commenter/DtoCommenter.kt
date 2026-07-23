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

import cn.enaium.jimmer.buddy.extensions.dto.BLOCK_COMMENT
import cn.enaium.jimmer.buddy.extensions.dto.LINE_COMMENT
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoTypes
import com.intellij.lang.CodeDocumentationAwareCommenter
import com.intellij.psi.PsiComment
import com.intellij.psi.tree.IElementType

/**
 * @author Enaium
 */
class DtoCommenter : CodeDocumentationAwareCommenter {
    override fun getLineCommentTokenType(): IElementType {
        return LINE_COMMENT
    }

    override fun getBlockCommentTokenType(): IElementType {
        return BLOCK_COMMENT
    }

    override fun getDocumentationCommentTokenType(): IElementType {
        return DtoTypes.DOC_COMMENT
    }

    override fun getDocumentationCommentPrefix(): String {
        return "/**"
    }

    override fun getDocumentationCommentLinePrefix(): String {
        return "*"
    }

    override fun getDocumentationCommentSuffix(): String {
        return "*/"
    }

    override fun isDocumentationComment(element: PsiComment): Boolean {
        return element.tokenType == DtoTypes.DOC_COMMENT
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