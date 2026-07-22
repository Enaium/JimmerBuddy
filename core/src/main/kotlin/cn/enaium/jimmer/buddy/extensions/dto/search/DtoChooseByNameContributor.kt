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

package cn.enaium.jimmer.buddy.extensions.dto.search

import cn.enaium.jimmer.buddy.extensions.dto.DtoFileType
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoType
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiFile
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoTypes
import com.intellij.navigation.ChooseByNameContributor
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.base.util.projectScope
import org.jetbrains.kotlin.idea.core.util.toPsiFile

/**
 * @author Enaium
 */
class DtoChooseByNameContributor : ChooseByNameContributor {
    override fun getNames(
        project: Project,
        includeNonProjectItems: Boolean
    ): Array<String> {

        val names = ArrayList<String>()

        FileTypeIndex.getFiles(
            DtoFileType,
            if (includeNonProjectItems) project.allScope() else project.projectScope()
        ).forEach {
            (it.toPsiFile(project) as? DtoPsiFile)?.let { file ->
                PsiTreeUtil.findChildrenOfType(file, DtoPsiDtoType::class.java).forEach { dtoType ->
                    names.add(dtoType.identifier.text)
                    dtoType.dtoBody.explicitPropList.forEach { prop ->
                        prop.positiveProp?.let { positiveProp ->
                            positiveProp.propName?.identifier?.text?.also { name -> names.add(name) }
                        }
                    }
                }
            }
        }
        return names.toTypedArray()
    }

    override fun getItemsByName(
        name: String,
        pattern: String,
        project: Project,
        includeNonProjectItems: Boolean
    ): Array<NavigationItem> {
        val items = mutableListOf<NavigationItem>()
        FileTypeIndex.getFiles(
            DtoFileType,
            if (includeNonProjectItems) project.allScope() else project.projectScope()
        ).forEach { dtoFile ->
            (dtoFile.toPsiFile(project) as? DtoPsiFile)?.let { file ->
                PsiTreeUtil.findChildrenOfType(file, DtoPsiDtoType::class.java).forEach { dtoType ->
                    if (dtoType.identifier.text.contains(name) == true
                        || dtoType.identifier.text.matches(pattern.toRegex()) == true
                    ) {
                        items.add(dtoType as NavigationItem)
                    }
                    dtoType.dtoBody.explicitPropList.forEach { prop ->
                        prop.positiveProp?.also { positiveProp ->
                            val propName = positiveProp.propName?.identifier?.text
                            if (propName?.contains(name) == true
                                || propName?.matches(pattern.toRegex()) == true
                            ) {
                                positiveProp.propName?.identifier?.also {
                                    items.add(it as NavigationItem)
                                }
                            }
                        }
                    }
                }
            }
        }
        return items.toTypedArray()
    }
}