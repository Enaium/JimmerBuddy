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

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoTypes.*
import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.TokenSet

/**
 * @author Enaium
 */
class DtoBlock(
    node: ASTNode,
    wrap: Wrap?,
    alignment: Alignment?,
    val spacingBuilder: SpacingBuilder
) : AbstractBlock(node, wrap, alignment) {
    override fun buildChildren(): List<DtoBlock> {
        return try {
            generateSequence(
                myNode::getFirstChildNode,
                ASTNode::getTreeNext
            ).filter { it.elementType != TokenType.WHITE_SPACE }.map { DtoBlock(it, wrap, null, spacingBuilder) }
                .toList()
        } catch (_: Throwable) {
            return emptyList()
        }
    }

    val body = TokenSet.create(
        DTO_BODY,
        ALIAS_GROUP,
        ENUM_BODY,
        TYPES_BLOCK
    )

    override fun getChildIndent(): Indent? {

        if (node.elementType in body) {
            return Indent.getNormalIndent()
        }

        return Indent.getNoneIndent()
    }

    override fun getIndent(): Indent? {
        if (node.elementType !in TokenSet.create(
                LBRACE,
                RBRACE,
                IMPLEMENTS
            ) && node.treeParent?.elementType in body
        ) {
            return Indent.getNormalIndent()
        }
        if (node.elementType == ARROW && node.treeParent?.elementType == EXPORT_STATEMENT) {
            return Indent.getNormalIndent()
        }

        return Indent.getNoneIndent()
    }

    override fun getSpacing(
        child1: Block?,
        child2: Block
    ): Spacing? {
        return spacingBuilder.getSpacing(this, child1, child2)
    }

    override fun isLeaf(): Boolean {
        return myNode.firstChildNode == null
    }
}