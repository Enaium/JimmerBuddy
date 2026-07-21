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