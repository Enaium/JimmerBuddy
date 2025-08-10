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

package cn.enaium.jimmer.buddy.extensions.completion

import cn.enaium.jimmer.buddy.storage.DatabaseCache
import cn.enaium.jimmer.buddy.utility.annotArgName
import cn.enaium.jimmer.buddy.utility.annotName
import cn.enaium.jimmer.buddy.utility.isEntity
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiClass
import com.intellij.psi.util.findParentOfType
import com.intellij.util.ProcessingContext
import org.babyfish.jimmer.sql.Table
import org.jetbrains.kotlin.psi.KtClass

/**
 * @author Enaium
 */
object TableCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.position
        if (position.annotName() != Table::class.qualifiedName) {
            return
        }

        if (position.annotArgName() != "name") {
            return
        }

        position.findParentOfType<PsiClass>()?.isEntity() == false && return
        position.findParentOfType<KtClass>()?.isEntity() == false && return

        DatabaseCache.getInstance(position.project).tables.forEach {
            result.addElement(LookupElementBuilder.create(it.name))
        }
    }
}