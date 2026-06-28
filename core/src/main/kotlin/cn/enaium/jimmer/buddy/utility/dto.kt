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

package cn.enaium.jimmer.buddy.utility

import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage
import cn.enaium.jimmer.buddy.extensions.dto.DtoLanguage.findChild
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoType
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiFile
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiProp
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiRoot
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiReference
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.findParentOfType
import org.babyfish.jimmer.internal.GeneratedBy
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.base.util.projectScope
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElementOfType

/**
 * @author Enaium
 */
fun Project.createDtoFile(content: String): DtoPsiFile {
    return PsiFileFactory.getInstance(this).createFileFromText("dummy.dto", DtoLanguage, content) as DtoPsiFile
}

fun Project.createDtoProp(name: String): DtoPsiProp? {
    return createDtoFile("Dummy { $name }").findChild("/dto/dtoType/dtoBody/explicitProp/*/prop")
}

fun DtoPsiDtoType.generatedName(): String? {
    val name = name?.value ?: return null
    val dtoPsiRoot = findParentOfType<DtoPsiRoot>() ?: return null
    val exportType = dtoPsiRoot.qualifiedName() ?: return null
    val exportPackage =
        dtoPsiRoot.exportStatement?.packageParts?.qualifiedName()
            ?: "${exportType.substringBeforeLast(".")}.dto"

    return "$exportPackage.$name"
}

fun DtoPsiDtoType.generatedReferences(): List<PsiElement> {
    val references = generatedReferenceSearchResults(project.projectScope())
        .map { it.element }
    if (references.isNotEmpty()) {
        return references
    }

    return generatedSourceReferenceElements(project.projectScope())
}

fun DtoPsiDtoType.generatedReferenceSearchResults(scope: SearchScope): List<PsiReference> {
    val target = JavaPsiFacade.getInstance(project)
        .findClass(generatedName() ?: return emptyList(), project.allScope())
        ?: return emptyList()
    val targetFiles = listOfNotNull(
        target.containingFile?.virtualFile,
        target.navigationElement.containingFile?.virtualFile,
        target.originalElement.containingFile?.virtualFile,
    ).toSet()

    return ReferencesSearch.search(target, scope)
        .mapNotNull { reference ->
            val virtualFile = reference.element.containingFile?.virtualFile ?: return@mapNotNull null
            reference.takeIf {
                virtualFile !in targetFiles &&
                    !virtualFile.path.isGeneratedPath() &&
                    !isJimmerGeneratedDtoFile(reference.element)
            }
        }
        .toList()
}

private fun DtoPsiDtoType.generatedSourceReferenceElements(scope: SearchScope): List<PsiElement> {
    val generatedName = generatedName() ?: return emptyList()
    val simpleName = generatedName.substringAfterLast(".")
    val references = linkedMapOf<String, PsiElement>()

    PsiSearchHelper.getInstance(project).processElementsWithWord(
        { element, offset ->
            val reference = element.findGeneratedSourceReference(simpleName, generatedName, offset)
            if (reference != null) {
                val file = reference.containingFile?.virtualFile
                if (file != null) {
                    references["${file.path}:${reference.textRange.startOffset}"] = reference
                }
            }
            true
        },
        scope,
        simpleName,
        UsageSearchContext.IN_CODE,
        true,
    )

    return references.values.toList()
}

private fun PsiElement.findGeneratedSourceReference(
    simpleName: String,
    generatedName: String,
    offset: Int,
): PsiElement? {
    val reference = findLeafAtRelativeOffset(offset)?.takeIf { it.text == simpleName }
        ?: findLeafWithText(simpleName)
        ?: return null
    if (!reference.isGeneratedSourceReference(generatedName)) {
        return null
    }

    return reference
}

private fun PsiElement.findLeafAtRelativeOffset(offset: Int): PsiElement? {
    val file = containingFile ?: return null
    val absoluteOffset = textRange.startOffset + offset
    return file.findElementAt(absoluteOffset)
}

private fun PsiElement.findLeafWithText(text: String): PsiElement? {
    if (firstChild == null) {
        return takeIf { it.text == text }
    }

    var child = firstChild
    while (child != null) {
        val found = child.findLeafWithText(text)
        if (found != null) {
            return found
        }
        child = child.nextSibling
    }

    return null
}

private fun PsiElement.isGeneratedSourceReference(generatedName: String): Boolean {
    val file = containingFile ?: return false
    if (file is DtoPsiFile) {
        return false
    }

    val virtualFile = file.virtualFile ?: return false
    if (!virtualFile.isUserSourceFile()) {
        return false
    }

    val text = file.text
    if (text.contains(generatedName)) {
        return true
    }

    val importText = "import $generatedName"
    if (text.contains(importText)) {
        return true
    }

    val generatedPackage = generatedName.substringBeforeLast(".")
    return file.packageName() == generatedPackage
}

private fun VirtualFile.isUserSourceFile(): Boolean {
    if (path.isGeneratedPath()) {
        return false
    }

    val extension = extension ?: return false
    return extension == "java" || extension == "kt"
}

private fun PsiElement.packageName(): String? {
    return when (this) {
        is PsiJavaFile -> packageName
        is KtFile -> packageFqName.asString()
        else -> null
    }
}

private fun String.isGeneratedPath(): Boolean {
    return split('/', '\\').any { it in generatedDirectoryNames }
}

private fun isJimmerGeneratedDtoFile(element: PsiElement): Boolean {
    val file = element.containingFile ?: return false
    val generatedByDto = file.toUElementOfType<UFile>()?.classes?.any { uClass ->
        uClass.uAnnotations.any { annotation ->
            if (annotation.qualifiedName != GeneratedBy::class.qualifiedName) {
                return@any false
            }

            annotation.findAttributeValue("file")?.evaluate()?.toString()?.endsWith(".dto") == true
        }
    } == true

    if (generatedByDto) {
        return true
    }

    val text = file.text
    return text.contains("GeneratedBy") && text.contains(".dto")
}

private val generatedDirectoryNames = setOf(
    "generated",
    "generated-sources",
    ".apt_generated",
)
