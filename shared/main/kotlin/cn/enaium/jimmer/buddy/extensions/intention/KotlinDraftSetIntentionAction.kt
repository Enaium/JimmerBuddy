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

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.utility.isDraft
import cn.enaium.jimmer.buddy.utility.runReadOnly
import cn.enaium.jimmer.buddy.utility.thread
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.idea.core.isOverridable
import org.jetbrains.kotlin.psi.KtLambdaExpression
import training.featuresSuggester.getParentOfType


/**
 * @author Enaium
 */
class KotlinDraftSetIntentionAction : DraftSetIntentionAction() {
    override fun invoke(
        project: Project,
        editor: Editor?,
        element: PsiElement,
    ) {

        var results = mutableListOf<String>()
        element.getParentOfType<KtLambdaExpression>()?.also { lambda ->
            editor?.also {
                caches[it.caretModel.offset]?.also { cache ->
                    results.addAll(cache)
                } ?: run {
                    val ktClass = thread { runReadOnly { JimmerBuddy.PSI_SHARED.receiver(lambda) } } ?: return@also
                    ktClass.getProperties().forEach {
                        if (it.isOverridable && it.isVar) {
                            results += "${it.name} = TODO()"
                        }
                    }
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
        return element is PsiWhiteSpace && element.getParentOfType<KtLambdaExpression>()?.let {
            thread { runReadOnly { JimmerBuddy.PSI_SHARED.receiver(it)?.isDraft() } }
        } == true
    }
}