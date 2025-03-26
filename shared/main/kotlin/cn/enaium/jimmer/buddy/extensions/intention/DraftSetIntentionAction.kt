package cn.enaium.jimmer.buddy.extensions.intention

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.utility.firstArg
import cn.enaium.jimmer.buddy.utility.isDraft
import cn.enaium.jimmer.buddy.utility.runReadOnly
import cn.enaium.jimmer.buddy.utility.thread
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.idea.core.isOverridable
import org.jetbrains.kotlin.psi.KtLambdaExpression
import training.featuresSuggester.getParentOfType


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
        return "Jimmer"
    }

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
        element.getParentOfType<PsiLambdaExpression>()?.also { lambda ->
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

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        element: PsiElement,
    ): Boolean {
        return element is PsiWhiteSpace && (element.getParentOfType<KtLambdaExpression>()?.let {
            thread { runReadOnly { JimmerBuddy.PSI_SHARED.receiver(it)?.isDraft() } }
        } == true || element.getParentOfType<PsiLambdaExpression>()?.firstArg()?.second?.isDraft() == true)
    }
}