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
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiAliasGroupBody
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiAliasPattern
import com.intellij.lang.ASTNode
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DtoPsiAliasGroupImpl(node: ASTNode) : ANTLRPsiNode(node), DtoPsiAliasGroup {
    override val pattern: DtoPsiAliasPattern?
        get() = findChild<DtoPsiAliasPattern>("/aliasGroup/aliasPattern")
    override val body: DtoPsiAliasGroupBody?
        get() = findChild<DtoPsiAliasGroupBody>("/aliasGroup/aliasGroupBody")
}