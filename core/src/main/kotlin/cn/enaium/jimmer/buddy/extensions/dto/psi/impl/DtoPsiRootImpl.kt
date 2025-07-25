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
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoType
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiExportStatement
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiImportStatement
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiRoot
import com.intellij.lang.ASTNode
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

/**
 * @author Enaium
 */
class DtoPsiRootImpl(node: ASTNode) : ANTLRPsiNode(node), DtoPsiRoot {
    override val exportStatement: DtoPsiExportStatement?
        get() = findChild<DtoPsiExportStatement>("/dto/exportStatement")

    override val importStatements: List<DtoPsiImportStatement>
        get() = findChildren("/dto/importStatement")

    override val dtoTypes: List<DtoPsiDtoType>
        get() = findChildren("/dto/dtoType")

    override fun hasImport(qualifiedName: String): Boolean {
        if (importStatements.find { it.qualifiedNameParts?.qualifiedName == qualifiedName } != null) {
            return true
        }

        val packageName = qualifiedName.substringBeforeLast(".")
        val typeName = qualifiedName.substringAfterLast(".")
        if (importStatements.find { it.qualifiedNameParts?.qualifiedName == packageName } != null) {
            return importStatements.find {
                it.importTypes.find { importedType ->
                    importedType.name?.value == typeName
                } != null
            } != null
        }
        return false
    }

    override fun qualifiedName(): String? {
        return exportStatement?.typeParts?.qualifiedName()
    }
}