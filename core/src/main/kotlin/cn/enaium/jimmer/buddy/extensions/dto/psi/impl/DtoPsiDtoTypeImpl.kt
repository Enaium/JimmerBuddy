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

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage.findChild
import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage.findChildren
import cn.enaium.jimmer.buddy.extensions.dto.psi.*
import cn.enaium.jimmer.buddy.utility.DTO_TYPE
import com.intellij.lang.ASTNode
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.util.findParentOfType
import org.jetbrains.kotlin.idea.base.util.allScope
import javax.swing.Icon

class DtoPsiDtoTypeImpl(node: ASTNode) : DtoPsiNamedElement(node), DtoPsiDtoType {
    override val modifiers: List<DtoPsiModifier>
        get() = findChildren<DtoPsiModifier>("/dtoType/modifier")
    override val name: DtoPsiName?
        get() = findChild<DtoPsiName>("/dtoType/name")
    override val body: DtoPsiDtoBody?
        get() = findChild<DtoPsiDtoBody>("/dtoType/dtoBody")

    override fun getName(): String = name?.value ?: "Unknown Name"

    override fun getIcon(flags: Int): Icon {
        return JimmerBuddy.Icons.Nodes.DTO_TYPE
    }

    override fun reference(): PsiElement? {
        val name = name?.value ?: return null
        val dtoPsiRoot = findParentOfType<DtoPsiRoot>() ?: return null
        val exportType = dtoPsiRoot.qualifiedName() ?: return null
        val exportPackage =
            dtoPsiRoot.exportStatement?.packageParts?.qualifiedName() ?: "${exportType.substringBeforeLast(".")}.dto"
        val target =
            JavaPsiFacade.getInstance(project).findClass("$exportPackage.$name", project.allScope())
                ?: return null
        return target
    }
}