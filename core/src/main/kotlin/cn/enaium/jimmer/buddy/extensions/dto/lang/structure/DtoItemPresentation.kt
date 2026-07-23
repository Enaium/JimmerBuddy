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
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiTypeRef
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoTypes
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import javax.swing.Icon

/**
 * @author Enaium
 */
class DtoItemPresentation(val element: PsiElement) : ItemPresentation {
    override fun getPresentableText(): String {
        return when (element) {
            is DtoPsiFile -> element.name
            is DtoPsiDtoType -> element.identifier.text.ifEmpty { "Unknown Name" }
            is DtoPsiExplicitProp -> element.positiveProp?.let { positiveProp ->
                val propName = positiveProp.propName?.identifier?.text
                val alias = positiveProp.alias?.identifier?.text
                if (propName != null) {
                    alias?.let { "$propName as $it" } ?: propName
                } else {
                    null
                }
            } ?: element.negativeProp?.let { negativeProp ->
                negativeProp.propName.identifier.text.let { "$it (Negative)" }
            } ?: element.userProp?.let { userProp ->
                val name = userProp.identifier.text
                userProp.typeRef?.let { typeRef ->
                    fun typeRefName(typeRef: DtoPsiTypeRef): String {
                        return "${typeRef.qualifiedName.text.substringAfterLast(".")}".let {
                            if (typeRef.genericArgumentList.isEmpty()) {
                                it
                            } else {
                                it + typeRef.genericArgumentList.joinToString(
                                    ", ",
                                    "<",
                                    ">"
                                ) { genericArgument ->
                                    genericArgument.typeRef?.let { typeRef -> typeRefName(typeRef) } ?: ""
                                }
                            }
                        }
                    }

                    "$name: ${typeRefName(typeRef)}"
                } ?: name
            } ?: "Unknow Name"

            else -> "Unknow Name"
        }
    }

    override fun getIcon(unused: Boolean): Icon {
        return element.getIcon(0)
    }
}