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
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiName
import cn.enaium.jimmer.buddy.utility.DTO_TYPE
import cn.enaium.jimmer.buddy.utility.generatedReferences
import cn.enaium.jimmer.buddy.utility.runReadOnly
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import java.awt.event.MouseEvent

/**
 * @author Enaium
 */
class DtoLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val dtoType = element.dtoType() ?: return
        val name = dtoType.name ?: return
        val anchor = name.firstChild ?: name
        if (element != anchor) {
            return
        }

        result.add(
            NavigationGutterIconBuilder.create(JimmerBuddy.Icons.Nodes.DTO_TYPE)
                .setTarget(name)
                .setTooltipText("Choose DTO Reference")
                .createLineMarkerInfo(anchor, DtoReferenceNavigationHandler(dtoType))
        )
    }

    private class DtoReferenceNavigationHandler(
        private val dtoType: DtoPsiDtoType,
    ) : GutterIconNavigationHandler<PsiElement> {
        override fun navigate(
            event: MouseEvent,
            elt: PsiElement,
        ) {
            val references = runReadOnly {
                if (dtoType.isValid) {
                    dtoType.generatedReferences()
                } else {
                    emptyList()
                }
            }
            DtoReferencePreview.show(references, event)
        }
    }

    private companion object {
        private fun PsiElement.dtoType(): DtoPsiDtoType? {
            if (this is DtoPsiDtoType) {
                return this
            }

            val name = when (this) {
                is DtoPsiName -> this
                else -> PsiTreeUtil.getParentOfType(this, DtoPsiName::class.java, false)
            } ?: return null

            val dtoType = name.parent as? DtoPsiDtoType ?: return null
            if (dtoType.name != name) {
                return null
            }

            return dtoType
        }
    }
}
