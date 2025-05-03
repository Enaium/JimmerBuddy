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
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiAliasGroup
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiExplicitProp
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiNegativeProp
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiPositiveProp
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiUserProp
import com.intellij.icons.AllIcons
import com.intellij.lang.ASTNode
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode
import javax.swing.Icon

class DtoPsiExplicitPropImpl(node: ASTNode) : ANTLRPsiNode(node), DtoPsiExplicitProp {
    override val aliasGroup: DtoPsiAliasGroup?
        get() = findChild<DtoPsiAliasGroup>("/explicitProp/aliasGroup")
    override val positiveProp: DtoPsiPositiveProp?
        get() = findChild<DtoPsiPositiveProp>("/explicitProp/positiveProp")
    override val negativeProp: DtoPsiNegativeProp?
        get() = findChild<DtoPsiNegativeProp>("/explicitProp/negativeProp")
    override val userProp: DtoPsiUserProp?
        get() = findChild<DtoPsiUserProp>("/explicitProp/userProp")

    override fun getIcon(flags: Int): Icon {
        return AllIcons.Nodes.Property
    }
}