package cn.enaium.jimmer.buddy.extensions.dto.completion

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.utility.IMMUTABLE
import cn.enaium.jimmer.buddy.utility.findCurrentImmutableType
import cn.enaium.jimmer.buddy.utility.isImmutable
import cn.enaium.jimmer.buddy.utility.psi
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElementOfType

/**
 * @author Enaium
 */
object TypesBranchCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val element = parameters.position
        findCurrentImmutableType(element)?.psi(element.project)?.also { psi ->
            psi.toUElementOfType<UClass>()?.javaPsi?.also { psiClass ->
                ClassInheritorsSearch.search(psiClass, element.project.allScope(), false)
                    .filter { it.isImmutable() }.forEach {
                        result.addElement(
                            LookupElementBuilder.create(it.name ?: return).withIcon(JimmerBuddy.Icons.IMMUTABLE)
                                .withInsertHandler { context, _ ->
                                    val project = context.project
                                    val editor = context.editor

                                    WriteCommandAction.runWriteCommandAction(project) {
                                        val tm = TemplateManager.getInstance(project)
                                        val template: Template = tm.createTemplate("", "")
                                        template.isToReformat = true
                                        template.addTextSegment(" {\n")
                                        template.addTextSegment("\t")
                                        template.addEndVariable()
                                        template.addTextSegment("\n}")
                                        tm.startTemplate(editor, template)
                                    }
                                })
                    }
            }
        }
    }
}