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
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoBody
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiPositiveProp
import cn.enaium.jimmer.buddy.utility.CommonImmutableType.CommonImmutableProp.Companion.type
import cn.enaium.jimmer.buddy.utility.PROP
import cn.enaium.jimmer.buddy.utility.findCurrentImmutableType
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.findParentOfType
import com.intellij.util.ProcessingContext

/**
 * @author Enaium
 */
object PropCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val element = parameters.position
        findCurrentImmutableType(element)?.props()?.forEach {
            result.addElement(
                LookupElementBuilder.create(it.name()).withIcon(JimmerBuddy.Icons.PROP)
                    .withTailText(" (from ${it.name()})").withTypeText(it.type().description)
            )
        }
    }
}

fun getTrace(position: PsiElement?): List<String> {
    val trace = mutableSetOf<String>()
    var parent: PsiElement? = position?.parent
    while (parent != null) {
        (parent.findParentOfType<DtoPsiDtoBody>()?.parent as? DtoPsiPositiveProp)?.prop?.value?.also { trace.add(it) }
        parent = parent.parent
    }
    return trace.reversed()
}