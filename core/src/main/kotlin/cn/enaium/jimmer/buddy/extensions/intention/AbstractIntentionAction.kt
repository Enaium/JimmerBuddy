/*
 * Copyright 2026 Enaium
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

package cn.enaium.jimmer.buddy.extensions.intention

import cn.enaium.jimmer.buddy.utility.workspace
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

/**
 * @author Enaium
 */
abstract class AbstractIntentionAction : PsiElementBaseIntentionAction() {
    final override fun invoke(
        project: Project,
        editor: Editor?,
        element: PsiElement
    ) {
        !element.project.workspace().isJimmerProject && return
        invokePsi(project, editor, element)
    }

    final override fun isAvailable(
        project: Project,
        editor: Editor?,
        element: PsiElement
    ): Boolean {
        !element.project.workspace().isJimmerProject && return false
        return isAvailablePsi(project, editor, element)
    }

    abstract fun invokePsi(
        project: Project,
        editor: Editor?,
        element: PsiElement
    )

    abstract fun isAvailablePsi(
        project: Project,
        editor: Editor?,
        element: PsiElement
    ): Boolean
}