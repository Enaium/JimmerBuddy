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

package cn.enaium.jimmer.buddy.extensions.dto.psi.impl

import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage.findChild
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiAliasPattern
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiNamedElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

/**
 * @author Enaium
 */
class DtoPsiAliasPatternImpl(node: ASTNode) : ANTLRPsiNode(node), DtoPsiAliasPattern {
    override val prefix: Boolean
        get() = children.find { it.text == "^" } != null
    override val suffix: Boolean
        get() = children.find { it.text == "$" } != null
    override val original: DtoPsiAliasPattern.Original?
        get() = findChild<DtoPsiAliasPattern.Original>("/aliasPattern/original")
    override val replacement: DtoPsiAliasPattern.Replacement?
        get() = findChild<DtoPsiAliasPattern.Replacement>("/aliasPattern/replacement")

    companion object {
        class OriginalImpl(node: ASTNode) : DtoPsiNamedElement(node), DtoPsiAliasPattern.Original {
            override val value: String
                get() = node.text

            override fun reference(): PsiElement? {
                return null
            }
        }

        class ReplacementImpl(node: ASTNode) : DtoPsiNamedElement(node), DtoPsiAliasPattern.Replacement {
            override val value: String
                get() = node.text

            override fun reference(): PsiElement? {
                return null
            }
        }
    }
}