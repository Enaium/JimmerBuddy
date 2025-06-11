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

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiImportStatement
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.util.findParentOfType
import com.intellij.util.ProcessingContext

/**
 * @author Enaium
 */
object ImportedTypeCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.position.project
        val packageName =
            parameters.position.findParentOfType<DtoPsiImportStatement>(true)?.qualifiedNameParts?.parts?.joinToString(".") { it.text }
                ?: return
        val classes =
            JavaPsiFacade.getInstance(project).findPackage(packageName)?.classes ?: emptyArray<PsiClass>()
        result.addAllElements(classes.map {
            LookupElementBuilder.create(it.name ?: "Unknown Name").withIcon(it.getIcon(0))
                .withTailText(" (from ${it.qualifiedName?.substringBeforeLast(".")})")
        })
    }
}