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
import cn.enaium.jimmer.buddy.extensions.dto.psi.*
import com.intellij.lang.ASTNode
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DtoPsiPositivePropImpl(node: ASTNode) : ANTLRPsiNode(node), DtoPsiPositiveProp {
    override val prop: DtoPsiProp?
        get() = findChild<DtoPsiProp>("/positiveProp/prop")
    override val alias: DtoPsiAlias?
        get() = findChild<DtoPsiAlias>("/positiveProp/alias")

    override val body: DtoPsiDtoBody?
        get() = findChild<DtoPsiDtoBody>("/positiveProp/dtoBody")
    override val modifier: DtoPsiModifier?
        get() = findChild<DtoPsiModifier>("/positiveProp/modifier")
}