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
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiRoot
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiPackage
import com.intellij.psi.util.findParentOfType
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.psi.psiUtil.endOffset

/**
 * @author Enaium
 */
object AnnotationQualifiedNameCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.position.project
        val parts = parameters.getParts()

        if (parts.isEmpty()) {
            val classes = FileBasedIndex.getInstance()
                .getAllKeys(JimmerBuddy.ANNOTATION_CLASS_INDEX, project)
                .mapNotNull { JavaPsiFacade.getInstance(project).findClass(it, project.allScope()) }
            result.addAllElements(classes.map {
                LookupElementBuilder.create(it.name ?: "Unknown Name").withInsertHandler { context, item ->
                    val root =
                        parameters.position.findParentOfType<DtoPsiRoot>() ?: return@withInsertHandler

                    if (!root.hasImport(it.qualifiedName!!)) {
                        parameters.editor.document.insertString(
                            root.importStatements.lastOrNull()?.endOffset ?: root.exportStatement?.endOffset ?: 0,
                            "\nimport ${it.qualifiedName}"
                        )
                    }

                }.withIcon(it.getIcon(0))
            })
        }

        val packageName = parts.joinToString(".")
        val subPackages =
            JavaPsiFacade.getInstance(project).findPackage(packageName)?.subPackages ?: emptyArray<PsiPackage>()
        result.addAllElements(subPackages.map {
            LookupElementBuilder.create(it.name ?: "Unknown Name").withIcon(AllIcons.Nodes.Package)
        })
        val classes =
            JavaPsiFacade.getInstance(project).findPackage(packageName)?.classes?.filter { it.isAnnotationType }
                ?: emptyList<PsiClass>()
        result.addAllElements(classes.map {
            LookupElementBuilder.create(it.name ?: "Unknown Name").withIcon(it.getIcon(0))
        })
    }
}