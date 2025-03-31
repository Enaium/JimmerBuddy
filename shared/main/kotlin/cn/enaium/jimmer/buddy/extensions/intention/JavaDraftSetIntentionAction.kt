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

package cn.enaium.jimmer.buddy.extensions.intention

import cn.enaium.jimmer.buddy.utility.firstArg
import cn.enaium.jimmer.buddy.utility.isDraft
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType


/**
 * @author Enaium
 */
class JavaDraftSetIntentionAction : DraftSetIntentionAction() {
    override fun invoke(
        project: Project,
        editor: Editor?,
        element: PsiElement,
    ) {

        var results = mutableListOf<String>()
        element.getParentOfType<PsiLambdaExpression>(true)?.also { lambda ->
            val (name, psiClass) = lambda.firstArg() ?: return@also
            psiClass?.methods?.forEach {
                if (it.name.startsWith("set")) {
                    results.add("${name}.${it.name}();")
                }
            }
        }
        if (results.isNotEmpty()) {
            editor?.insertLines(results)
        }
    }

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        element: PsiElement,
    ): Boolean {
        return element is PsiWhiteSpace && element.getParentOfType<PsiLambdaExpression>(true)
            ?.firstArg()?.second?.isDraft() == true
    }
}