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

package cn.enaium.jimmer.buddy.extensions.insight

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.utility.isImmutable
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.searches.ClassInheritorsSearch
import org.babyfish.jimmer.sql.MappedSuperclass
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElementOfType
import java.awt.event.MouseEvent

/**
 * @author Enaium
 */
class ImmutableInheritorCodeVisionProvider : ImmutableCodeVisionProvider() {

    override val id: String
        get() = "JimmerBuddy.immutable.inheritor"
    override val name: String
        get() = "Immutable Inheritor"

    override fun getHint(element: PsiElement, file: PsiFile): String? {
        val uClass = element.toUElementOfType<UClass>() ?: return null
        uClass.uAnnotations.find { it.qualifiedName == MappedSuperclass::class.qualifiedName } ?: return null

        val count = findInheritors(uClass.javaPsi).size

        return if (count > 0) {
            "$count Inheritors"
        } else {
            null
        }
    }

    override fun handleClick(
        editor: Editor,
        element: PsiElement,
        event: MouseEvent?
    ) {
        val uClass = element.toUElementOfType<UClass>() ?: return
        JimmerBuddy.Services.NAVIGATION.getPsiElementPopup(
            findInheritors(uClass.javaPsi),
            "Choose Inheritors"
        ).showInBestPositionFor(editor)
    }

    private fun findInheritors(psiClass: PsiClass): List<PsiClass> {
        return ClassInheritorsSearch.search(psiClass, psiClass.project.allScope(), false)
            .filter { it.isImmutable() }
    }
}