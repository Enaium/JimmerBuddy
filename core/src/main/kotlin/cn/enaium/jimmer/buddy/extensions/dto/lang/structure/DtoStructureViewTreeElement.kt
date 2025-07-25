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

package cn.enaium.jimmer.buddy.extensions.dto.lang.structure

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoType
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiExplicitProp
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiFile
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiRoot
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.NavigationItem
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.util.childrenOfType

/**
 * @author Enaium
 */
class DtoStructureViewTreeElement(val element: PsiElement) : StructureViewTreeElement {
    override fun getValue(): Any {
        return element
    }

    override fun getPresentation(): ItemPresentation {
        return DtoItemPresentation(element)
    }

    override fun getChildren(): Array<TreeElement> {
        return when (element) {
            is DtoPsiFile -> element.childrenOfType<DtoPsiRoot>().firstOrNull()?.childrenOfType<DtoPsiDtoType>()
            is DtoPsiDtoType -> element.body?.explicitProps?.filter { explicitProps -> explicitProps.aliasGroup == null }
            is DtoPsiExplicitProp -> element.positiveProp?.body?.explicitProps
            else -> emptyList<PsiElement>()
        }?.map { DtoStructureViewTreeElement(it) }?.toTypedArray() ?: emptyArray()
    }

    override fun navigate(requestFocus: Boolean) {
        (element as? NavigatablePsiElement)?.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean {
        return element is NavigationItem && element.canNavigate()
    }

    override fun canNavigateToSource(): Boolean {
        return element is NavigationItem && element.canNavigateToSource()
    }
}