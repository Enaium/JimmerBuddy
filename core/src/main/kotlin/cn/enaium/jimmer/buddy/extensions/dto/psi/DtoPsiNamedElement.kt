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

package cn.enaium.jimmer.buddy.extensions.dto.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

/**
 * @author Enaium
 */
abstract class DtoPsiNamedElement(node: ASTNode) : ANTLRPsiNode(node), DtoPsiElement, PsiNameIdentifierOwner {
    override fun setName(name: String): DtoPsiElement {
        return this
    }

    override fun getNameIdentifier(): PsiElement {
        return this
    }

    override fun getReference(): PsiReference {
        return object : PsiReferenceBase<DtoPsiNamedElement>(this, firstChild.textRangeInParent) {
            override fun resolve(): PsiElement? {
                return reference()
            }
        }
    }

    abstract fun reference(): PsiElement?
}