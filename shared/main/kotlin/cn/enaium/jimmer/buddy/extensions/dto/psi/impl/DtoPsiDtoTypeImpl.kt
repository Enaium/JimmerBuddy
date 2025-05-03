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
import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage.findChildren
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoBody
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoType
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiModifier
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiName
import com.intellij.icons.AllIcons
import com.intellij.lang.ASTNode
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode
import javax.swing.Icon

class DtoPsiDtoTypeImpl(node: ASTNode) : ANTLRPsiNode(node), DtoPsiDtoType {
    override val modifiers: List<DtoPsiModifier>
        get() = findChildren<DtoPsiModifier>("/dtoType/modifier")
    override val name: DtoPsiName?
        get() = findChild<DtoPsiName>("/dtoType/name")
    override val body: DtoPsiDtoBody?
        get() = findChild<DtoPsiDtoBody>("/dtoType/dtoBody")

    override fun getIcon(flags: Int): Icon {
        return AllIcons.Nodes.Type
    }
}