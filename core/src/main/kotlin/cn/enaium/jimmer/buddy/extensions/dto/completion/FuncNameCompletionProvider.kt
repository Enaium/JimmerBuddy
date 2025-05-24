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

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoType
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.util.findParentOfType
import com.intellij.util.ProcessingContext
import org.babyfish.jimmer.dto.compiler.Constants
import org.babyfish.jimmer.dto.compiler.DtoModifier

/**
 * @author Enaium
 */
object FuncNameCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val functions = mutableListOf<String>()
        if (parameters.position.findParentOfType<DtoPsiDtoType>()?.modifiers?.map { it.value }
                ?.contains(DtoModifier.SPECIFICATION.name.lowercase()) == true) {
            functions.addAll(Constants.QBE_FUNC_NAMES)
        } else {
            functions.add("id")
        }
        functions.add("flat")
        functions.forEach {
            result.addElement(
                LookupElementBuilder.create("$it()").withIcon(AllIcons.Nodes.Function)
                    .withTypeText("Function")
            )
        }
    }
}