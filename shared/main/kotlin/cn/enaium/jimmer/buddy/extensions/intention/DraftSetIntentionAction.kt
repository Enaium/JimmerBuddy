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

import cn.enaium.jimmer.buddy.utility.*
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.idea.core.isOverridable
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

/**
 * @author Enaium
 */
class DraftSetIntentionAction : PsiElementBaseIntentionAction() {

    companion object {
        val caches = mutableMapOf<Int, List<String>>()
    }

    override fun getText(): String {
        return "Generate all set of the draft"
    }

    override fun getFamilyName(): String {
        return "Generate all set of the draft"
    }

    fun Editor.insertLines(lines: List<String>) {
        val caretOffset = caretModel.offset
        val caretLine = caretModel.logicalPosition.line
        val lineStartOffset = document.getLineStartOffset(caretLine)
        val lineText = document.text.substring(lineStartOffset, caretOffset)
        val indentation = lineText.takeWhile { it.isWhitespace() }
        val indentedResults = lines.joinToString("\n") { if (it == lines.first()) it else "$indentation$it" }
        if (caches.containsKey(caretOffset)) {
            caches.remove(caretOffset)
        } else {
            caches[caretOffset] = lines
        }
        document.insertString(caretOffset, indentedResults)
    }

    override fun invoke(
        project: Project,
        editor: Editor?,
        element: PsiElement
    ) {
        var results = mutableListOf<String>()
        element.getParentOfType<KtLambdaExpression>(true)?.also { lambda ->
            editor?.also {
                caches[it.caretModel.offset]?.also { cache ->
                    results.addAll(cache)
                } ?: run {
                    val ktClass = thread { runReadOnly { lambda.receiver() } } ?: return@also
                    ktClass.getProperties().forEach {
                        if (it.isOverridable && it.isVar) {
                            results += "${it.name} = TODO()"
                        }
                    }
                }
            }
        }
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

    fun isJavaAvailable(element: PsiElement): Boolean {
        return element is PsiWhiteSpace && element.getParentOfType<PsiLambdaExpression>(true)
            ?.firstArg()?.second?.isDraft() == true
    }

    fun isKotlinAvailable(element: PsiElement): Boolean {
        return element is PsiWhiteSpace && element.getParentOfType<KtLambdaExpression>(true)?.let {
            thread { runReadOnly { it.receiver()?.isDraft() } }
        } == true
    }

    override fun isAvailable(
        p0: Project,
        p1: Editor?,
        element: PsiElement
    ): Boolean {
        return isJavaAvailable(element) || isKotlinAvailable(element)
    }
}