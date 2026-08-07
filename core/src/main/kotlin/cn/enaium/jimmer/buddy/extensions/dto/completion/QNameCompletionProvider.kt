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
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiExportStatement
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiImportStatement
import cn.enaium.jimmer.buddy.extensions.index.ClassKindIndex
import cn.enaium.jimmer.buddy.utility.name
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiPackage
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.psi.psiUtil.endOffset

/**
 * @author Enaium
 */
open class QNameCompletionProvider(
    val kind: ClassKindIndex.Kind,
    val useImportHandler: Boolean = true
) : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.position.project
        val parts = parameters.getParts()

        if (parts.isEmpty()) {
            val index = FileBasedIndex.getInstance()
            val classes = index
                .getAllKeys(JimmerBuddy.Indexes.CLASS_KIND, project)
                .filter {
                    index.getValues(JimmerBuddy.Indexes.CLASS_KIND, it, project.allScope()).contains(kind)
                }
                .mapNotNull { JavaPsiFacade.getInstance(project).findClass(it, project.allScope()) }
            result.addAllElements(classes.map { clazz ->
                val lookupName = clazz.name ?: "Unknown Name"
                val builder = LookupElementBuilder.create(lookupName)
                    .withTailText(" (from ${clazz.qualifiedName?.substringBeforeLast(".") ?: ""})")
                    .withIcon(clazz.getIcon(0))
                if (useImportHandler) {
                    builder.withInsertHandler { _, _ ->
                        val file = parameters.position.containingFile ?: return@withInsertHandler
                        if (!hasImport(file, clazz.qualifiedName!!)) {
                            val importStatements = PsiTreeUtil.getChildrenOfType(file, DtoPsiImportStatement::class.java)
                            val exportStatement = PsiTreeUtil.findChildOfType(file, DtoPsiExportStatement::class.java)
                            parameters.editor.document.insertString(
                                importStatements?.lastOrNull()?.endOffset ?: exportStatement?.endOffset ?: 0,
                                "\nimport ${clazz.qualifiedName}"
                            )
                        }
                    }
                } else {
                    builder.withInsertHandler { context, _ ->
                        context.document.replaceString(
                            context.startOffset,
                            context.tailOffset,
                            clazz.qualifiedName ?: lookupName
                        )
                    }
                }
            })
        }

        val packageName = parts.joinToString(".")
        val subPackages =
            JavaPsiFacade.getInstance(project).findPackage(packageName)?.subPackages ?: emptyArray<PsiPackage>()
        result.addAllElements(subPackages.map {
            LookupElementBuilder.create(it.name ?: "Unknown Name").withIcon(AllIcons.Nodes.Package)
        })

        if (parts.size > 1) {
            val index = FileBasedIndex.getInstance()
            val classes = JavaPsiFacade.getInstance(project).findPackage(packageName)?.classes?.toList() ?: emptyList()
            val filteredClasses = classes.filter { clazz ->
                val qualifiedName = clazz.qualifiedName ?: return@filter false
                index.getValues(JimmerBuddy.Indexes.CLASS_KIND, qualifiedName, project.allScope()).contains(kind)
            }
            result.addAllElements(filteredClasses.map {
                LookupElementBuilder.create(it.name ?: "Unknown Name").withIcon(it.getIcon(0))
            })
        }
    }

    private fun hasImport(file: com.intellij.psi.PsiFile, qualifiedName: String): Boolean {
        val importStatements =
            PsiTreeUtil.getChildrenOfType(file, DtoPsiImportStatement::class.java) ?: return false
        return importStatements.any { stmt ->
            val qname = stmt.qualifiedName.name()
            qname == qualifiedName
        }
    }
}