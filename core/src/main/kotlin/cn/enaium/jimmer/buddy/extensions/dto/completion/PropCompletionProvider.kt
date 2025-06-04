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
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiRoot
import cn.enaium.jimmer.buddy.utility.CommonImmutableType.CommonImmutableProp.Companion.type
import cn.enaium.jimmer.buddy.utility.isJavaProject
import cn.enaium.jimmer.buddy.utility.isKotlinProject
import cn.enaium.jimmer.buddy.utility.toCommonImmutableType
import cn.enaium.jimmer.buddy.utility.toImmutable
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.util.findParentOfType
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.KtClass

/**
 * @author Enaium
 */
object PropCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.position.project
        val trace = getTrace(parameters.position)
        val typeName =
            parameters.position.findParentOfType<DtoPsiRoot>()?.qualifiedName() ?: return
        val commonImmutable = if (project.isJavaProject()) {
            JavaPsiFacade.getInstance(project).findClass(typeName, project.allScope())?.toImmutable()
                ?.toCommonImmutableType() ?: return
        } else if (project.isKotlinProject()) {
            (KotlinFullClassNameIndex[typeName, project, project.allScope()].firstOrNull() as? KtClass)?.toImmutable()
                ?.toCommonImmutableType() ?: return
        } else {
            return
        }

        var currentImmutable = commonImmutable

        trace.forEach { trace ->
            currentImmutable.props().find { it.name() == trace }?.targetType()?.also {
                currentImmutable = it
            }
        }

        currentImmutable.props().forEach {
            result.addElement(
                LookupElementBuilder.create(it.name()).withIcon(JimmerBuddy.Icons.PROP)
                    .withTailText(" (from ${currentImmutable.name()})").withTypeText(it.type().description)
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