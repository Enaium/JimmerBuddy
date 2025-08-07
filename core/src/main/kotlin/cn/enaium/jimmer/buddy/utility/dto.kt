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
import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage.findChild
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiFile
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiProp
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory

/**
 * @author Enaium
 */
fun Project.createDtoFile(content: String): DtoPsiFile {
    return PsiFileFactory.getInstance(this).createFileFromText("dummy.dto", DtoLanguage, content) as DtoPsiFile
}

fun Project.createDtoProp(name: String): DtoPsiProp? {
    return createDtoFile("Dummy { $name }").findChild("/dto/dtoType/dtoBody/explicitProp/*/prop")
}