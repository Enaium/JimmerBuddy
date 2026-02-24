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
import com.intellij.openapi.application.EDT
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.withBackgroundProgress
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiWhiteSpace
import kotlinx.coroutines.*
import org.jetbrains.kotlin.idea.core.isOverridable
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

/**
 * @author Enaium
 */
class DraftSetIntentionAction : AbstractIntentionAction() {

    override fun getText(): String {
        return I18n.message("intention.allDraftSet")
    }

    override fun getFamilyName(): String {
        return "Generate all set of the draft"
    }

    fun Editor.insertLines(lines: List<String>) {
        val project = project ?: return
        val caretOffset = caretModel.offset
        val caretLine = caretModel.logicalPosition.line
        val lineStartOffset = document.getLineStartOffset(caretLine)
        val lineText = document.text.substring(lineStartOffset, caretOffset)
        val indentation = lineText.takeWhile { it.isWhitespace() }
        val indentedResults = lines.joinToString("\n") { if (it == lines.first()) it else "$indentation$it" }
        WriteCommandAction.writeCommandAction(project).run<Throwable> {
            document.insertString(caretOffset, indentedResults)
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
    }

    override fun invokePsi(
        project: Project,
        editor: Editor?,
        element: PsiElement
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            withBackgroundProgress(project, "Generating all set") {
                supervisorScope {
                    val job = launch {
                        project.readActionSmartCoroutine {
                            val results = mutableListOf<String>()
                            element.getParentOfType<KtLambdaExpression>(true)?.also { lambda ->
                                editor?.also {
                                    val ktClass = lambda.receiver() ?: return@also
                                    ktClass.getProperties().forEach {
                                        if (it.isOverridable && it.isVar) {
                                            results += "${it.name} = TODO()"
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
                                CoroutineScope(Dispatchers.EDT).launch {
                                    editor?.insertLines(results)
                                }
                            }
                        }
                    }

                    job.invokeOnCompletion { ex ->
                        if (ex != null && ex !is CancellationException && ex !is ControlFlowException) {
                            element.project.workspace().log.error(ex)
                        }
                    }
                }
            }
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

    override fun isAvailablePsi(project: Project, editor: Editor?, element: PsiElement): Boolean {
        return isJavaAvailable(element) || isKotlinAvailable(element)
    }
}