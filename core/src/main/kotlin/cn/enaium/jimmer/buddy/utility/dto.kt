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

package cn.enaium.jimmer.buddy.utility

import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage
import cn.enaium.jimmer.buddy.extensions.dto.psi.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.psiUtil.children

/**
 * @author Enaium
 */
fun Project.createDtoFile(content: String): DtoPsiFile {
    return PsiFileFactory.getInstance(this).createFileFromText("dummy.dto", DtoLanguage, content) as DtoPsiFile
}

fun Project.createDtoProp(name: String): String? {
    val file = createDtoFile("Dummy { $name }")
    val dtoType = PsiTreeUtil.findChildOfType(file, DtoPsiDtoType::class.java) ?: return null
    val dtoBody = dtoType.dtoBody
    return dtoBody.explicitPropList.firstOrNull()
        ?.positiveProp?.let { positiveProp ->
            positiveProp.children.firstOrNull { it.node.elementType == DtoTypes.IDENTIFIER }?.text
        }
}

fun Project.createDtoTypeName(name: String): PsiElement {
    val file = PsiFileFactory.getInstance(this).createFileFromText(
        "dummy.dto",
        DtoLanguage,
        "$name {}"
    ) as DtoPsiFile

    val dtoType = PsiTreeUtil.findChildOfType(file, DtoPsiDtoType::class.java)
        ?: error("Cannot create dto type")

    return dtoType.identifier
}

fun DtoPsiDtoType.generatedName(): String? {
    val name = identifier.text ?: return null
    val file = containingFile ?: return null
    val exportStatement = PsiTreeUtil.findChildOfType(file, DtoPsiExportStatement::class.java) ?: return null
    val exportType = exportStatement.qualifiedName.name()
    val exportPackage = exportStatement.exportPackage?.qualifiedName?.name()
        ?: "${exportType.substringBeforeLast(".")}.dto"
    return "$exportPackage.$name"
}

fun DtoPsiQualifiedName.name(): String {
    return node.children().joinToString("") { it.text }
}