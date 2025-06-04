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

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiPositiveProp
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiRoot
import cn.enaium.jimmer.buddy.utility.isJavaProject
import cn.enaium.jimmer.buddy.utility.isKotlinProject
import cn.enaium.jimmer.buddy.utility.toCommonImmutableType
import cn.enaium.jimmer.buddy.utility.toImmutable
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.util.findParentOfType
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

/**
 * @author Enaium
 */
object EnumEntryCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.position.project
        val prop = parameters.position.findParentOfType<DtoPsiPositiveProp>() ?: return
        val trace = getTrace(prop)
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

        val enumName = currentImmutable.props().find { it.name() == prop.prop?.value }?.typeName() ?: return

        val entries = if (project.isJavaProject()) {
            JavaPsiFacade.getInstance(project).findClass(enumName, project.allScope())
                ?.getChildrenOfType<PsiEnumConstant>()
                ?.map { it.name } ?: emptyList()
        } else if (project.isKotlinProject()) {
            (KotlinFullClassNameIndex[enumName, project, project.allScope()].firstOrNull() as? KtClass)?.getChildOfType<KtClassBody>()
                ?.getChildrenOfType<KtEnumEntry>()?.map { it.name ?: "Unknown Name" } ?: emptyList()
        } else {
            emptyList()
        }

        result.addAllElements(entries.map {
            LookupElementBuilder.create(it).withTypeText(enumName.substringAfterLast(".")).withIcon(AllIcons.Nodes.Enum)
        })
    }
}