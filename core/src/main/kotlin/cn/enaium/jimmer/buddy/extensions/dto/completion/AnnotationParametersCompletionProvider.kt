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

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiAnnotation
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiAnnotationArguments
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiAnnotationNamedArgument
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiImportStatement
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoTypes
import cn.enaium.jimmer.buddy.utility.name
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.psi.util.findParentOfType
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.base.util.allScope

/**
 * @author Enaium
 */
object AnnotationParametersCompletionProvider : CompletionProvider<CompletionParameters>() {

    private val BUILT_IN_ANNOTATION_METHODS = setOf(
        "equals", "hashCode", "toString", "annotationType"
    )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val element = parameters.position
        val project = element.project

        // 1. Find the parent annotation from the current position
        val annotation = element.findParentOfType<DtoPsiAnnotation>() ?: return

        // 2. Get the qualified name of the annotation as written in the DTO file
        val qualifiedName = annotation.qualifiedName.text ?: return

        // 3. Resolve the annotation qualified name (handle short names via imports)
        val resolvedName = resolveAnnotationQualifiedName(annotation, qualifiedName) ?: return

        // 4. Resolve to the PSI class of the annotation (Java or Kotlin)
        val psiClass = JavaPsiFacade.getInstance(project).findClass(resolvedName, project.allScope()) ?: return
        if (!psiClass.isAnnotationType) return

        // 5. Collect all attribute names declared in the annotation class
        val allParameterNames = psiClass.methods
            .filter { it.parameterList.isEmpty }
            .map { it.name }
            .filterNot { it in BUILT_IN_ANNOTATION_METHODS }
            .toSet()

        // 6. Get parameter names that have already been written in the current annotation
        val writtenNames = getWrittenParameterNames(annotation, element)

        // 7. Compute available parameter names (exclude already written ones)
        val available = allParameterNames - writtenNames

        // 8. Create lookup elements for completion
        result.addAllElements(available.map { paramName ->
            LookupElementBuilder.create(paramName).withTailText(" = ")
        })
    }

    /**
     * Resolve the annotation qualified name, handling short names via imports.
     *
     * For example, if the annotation is written as `@Immutable` and there is
     * an import `org.babyfish.jimmer.kt.dto.Immutable`, the resolved name
     * will be `org.babyfish.jimmer.kt.dto.Immutable`.
     */
    private fun resolveAnnotationQualifiedName(
        annotation: DtoPsiAnnotation,
        qualifiedName: String
    ): String? {
        // When the name already contains dots, it is already a fully qualified name
        if (qualifiedName.contains(".")) {
            return qualifiedName
        }

        // For short names, resolve via imports in the DTO file
        val file = annotation.containingFile ?: return null
        val shortName = qualifiedName.substringAfterLast(".")

        val importStatements = PsiTreeUtil.getChildrenOfType(file, DtoPsiImportStatement::class.java) ?: return null

        for (importStatement in importStatements) {
            val importQName = importStatement.qualifiedName.name()

            if (importStatement.importedTypeList.isEmpty()) {
                // Direct import: `import com.example.AnnotationName`
                if (importQName.substringAfterLast(".") == shortName) {
                    return importQName
                }
            } else {
                // Package import: `import com.example { AnnotationName, Other }`
                if (importStatement.importedTypeList.any {
                        it.identifier.text == shortName
                    }) {
                    return "$importQName.$shortName"
                }
            }
        }

        return null
    }

    /**
     * Collect the names of annotation parameters that have already been written
     * in the current annotation arguments, excluding the parameter being currently edited.
     */
    private fun getWrittenParameterNames(
        annotation: DtoPsiAnnotation,
        currentElement: PsiElement
    ): Set<String> {
        val currentNamedArg = currentElement.findParentOfType<DtoPsiAnnotationNamedArgument>()

        return annotation.children
            .asSequence()
            .filterIsInstance<DtoPsiAnnotationArguments>()
            .flatMap { it.annotationArgumentList.toList() }
            .mapNotNull { it.annotationNamedArgument }
            .filter { it != currentNamedArg }
            .mapNotNull { namedArg ->
                namedArg.identifier.text
            }
            .toSet()
    }
}