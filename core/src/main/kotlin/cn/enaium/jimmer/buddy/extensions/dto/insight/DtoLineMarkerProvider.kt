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

package cn.enaium.jimmer.buddy.extensions.dto.insight

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoType
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoTypes
import cn.enaium.jimmer.buddy.utility.DTO_TYPE
import cn.enaium.jimmer.buddy.utility.generatedName
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.base.util.allScope

/**
 * @author Enaium
 */
class DtoLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (element.node.elementType != DtoTypes.IDENTIFIER)
            return

        val dtoType = element.parent as? DtoPsiDtoType ?: return

        val target =
            JavaPsiFacade.getInstance(dtoType.project)
                .findClass(dtoType.generatedName() ?: return, dtoType.project.allScope())
                ?: return

        result.add(
            NavigationGutterIconBuilder.create(JimmerBuddy.Icons.Nodes.DTO_TYPE).setTargets(listOf(target))
                .createLineMarkerInfo(dtoType)
        )
    }
}