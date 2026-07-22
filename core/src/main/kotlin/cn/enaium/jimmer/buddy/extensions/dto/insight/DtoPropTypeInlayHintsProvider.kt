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

package cn.enaium.jimmer.buddy.extensions.dto.insight

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoType
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiFoldProp
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiNegativeProp
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiPositiveProp
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiTypeBranch
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiUserProp
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoTypes
import cn.enaium.jimmer.buddy.utility.endOffset
import cn.enaium.jimmer.buddy.utility.generatedName
import cn.enaium.jimmer.buddy.utility.resolveClass
import cn.enaium.jimmer.buddy.utility.resolveGenericsClassInType
import cn.enaium.jimmer.buddy.utility.type
import cn.enaium.jimmer.buddy.utility.workspace
import com.intellij.codeInsight.hints.declarative.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.util.elementType
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.findPropertyByName

/**
 * @author Enaium
 */
class DtoPropTypeInlayHintsProvider : InlayHintsProvider {

    val primitiveTypes = listOf(
        "long",
        "int",
        "short",
        "byte",
        "char",
        "float",
        "double",
    )

    val boxedTypes = listOf(
        "Long",
        "Integer",
        "Short",
        "Byte",
        "Char",
        "Float",
        "Double",
    )

    override fun createCollector(
        file: PsiFile,
        editor: Editor
    ): SharedBypassCollector {
        return object : SharedBypassCollector {
            override fun collectFromElement(
                element: PsiElement,
                sink: InlayTreeSink
            ) {
                // Detect IDENTIFIER tokens inside prop contexts
                if (element.elementType != DtoTypes.IDENTIFIER) return
                val parent = element.parent
                if (parent !is DtoPsiPositiveProp && parent !is DtoPsiNegativeProp &&
                    parent !is DtoPsiUserProp && parent !is DtoPsiFoldProp) return

                val propName = when (parent) {
                    is DtoPsiPositiveProp -> parent.propName?.identifier?.text
                    is DtoPsiNegativeProp -> parent.propName.identifier.text
                    is DtoPsiUserProp -> parent.identifier.text
                    is DtoPsiFoldProp -> parent.identifier.text
                    else -> null
                } ?: return

                val project = element.project

                // Resolve the reference (PsiMethod or KtNamedDeclaration)
                val reference = resolvePropReference(element, propName, project) ?: return

                val target = when (reference) {
                    is PsiMethod -> reference.returnType?.resolveGenericsClassInType()?.let { generic ->
                        if (generic.substitutor != PsiSubstitutor.EMPTY) {
                            generic.element?.typeParameters?.first()
                                ?.let { generic.substitutor.substitute(it)?.resolveClass() }
                        } else {
                            reference.returnType?.resolveClass()
                        }
                    }

                    is KtNamedDeclaration -> (reference as? KtProperty)?.typeReference?.type()?.let {
                        if (it.arguments.isEmpty()) {
                            it.ktClass
                        } else {
                            it.arguments.first().ktClass
                        }
                    }

                    else -> return
                }
                val text = when (reference) {
                    is PsiMethod -> {
                        reference.returnType?.presentableText?.let {
                            if (reference.modifierList.annotations.any { annotation ->
                                    annotation.qualifiedName?.substringAfterLast(".")?.startsWith("Null") == true
                                }) {
                                "$it?"
                            } else if (it in boxedTypes) {
                                "$it?"
                            } else if (it in primitiveTypes) {
                                "$it!"
                            } else {
                                it
                            }
                        }
                    }

                    is KtNamedDeclaration -> {
                        (reference as? KtProperty)?.typeReference?.text
                    }

                    else -> {
                        return
                    }
                } ?: return

                val position = InlineInlayPosition(element.endOffset, relatedToPrevious = true)
                sink.addPresentation(position, hasBackground = true) {
                    text(
                        text, InlayActionData(
                            PsiPointerInlayActionPayload((target ?: element).createSmartPointer()),
                            PsiPointerInlayActionNavigationHandler.HANDLER_ID
                        )
                    )
                }
            }
        }
    }
}

private fun resolvePropReference(element: PsiElement, propName: String, project: com.intellij.openapi.project.Project): PsiElement? {
    // Build trace from the parent chain (excluding the current prop)
    val trace = mutableListOf<String>()
    var current: PsiElement? = element.parent?.parent
    while (current != null) {
        when (current) {
            is DtoPsiPositiveProp -> {
                current.propName?.identifier?.text?.also { trace.add(it) }
            }
            is DtoPsiTypeBranch -> {
                current.qualifiedName.text.split(".").lastOrNull()?.also { trace.add(it) }
            }
        }
        current = current.parent
    }
    trace.reverse()

    // Find the root DTO type and get the generated name (FQN of the target entity)
    val dtoType = PsiTreeUtil.findChildOfType(element.containingFile, DtoPsiDtoType::class.java) ?: return null
    val generatedName = dtoType.generatedName() ?: return null

    // Resolve the entity class
    val entityClass: PsiElement? = if (project.workspace().isJavaProject) {
        JavaPsiFacade.getInstance(project).findClass(generatedName, project.allScope())
    } else if (project.workspace().isKotlinProject) {
        (KotlinFullClassNameIndex[generatedName, project, project.allScope()].firstOrNull() as? KtClass)
    } else {
        null
    } ?: return null

    // Navigate the trace to find the current type containing the property
    var currentType: PsiElement? = entityClass
    for (traceName in trace) {
        currentType = findPropertyInClass(currentType ?: return null, traceName, project) ?: return null
        currentType = resolvePropertyTargetType(currentType ?: return null, project) ?: return null
    }

    // Find the target property by name
    return findPropertyInClass(currentType ?: return null, propName, project)
}

private fun findPropertyInClass(psiClass: PsiElement, name: String, project: com.intellij.openapi.project.Project): PsiElement? {
    return when (psiClass) {
        is PsiClass -> psiClass.allMethods.find { it.name == name }
        is KtClass -> psiClass.findPropertyByName(name)
        else -> null
    }
}

private fun resolvePropertyTargetType(prop: PsiElement, project: com.intellij.openapi.project.Project): PsiElement? {
    return when (prop) {
        is PsiMethod -> {
            prop.returnType?.resolveGenericsClassInType()?.let { generic ->
                if (generic.substitutor != PsiSubstitutor.EMPTY) {
                    generic.element?.typeParameters?.firstOrNull()
                        ?.let { generic.substitutor.substitute(it)?.resolveClass() }
                } else {
                    val psiClass = generic.element
                    if (psiClass != null && psiClass.typeParameters.isNotEmpty()) {
                        (generic.element as? PsiClass)?.let { it }
                    } else {
                        generic.element
                    }
                }
            }
        }
        is KtProperty -> {
            prop.typeReference?.type()?.let {
                if (it.arguments.isEmpty()) {
                    it.ktClass
                } else {
                    it.arguments.first().ktClass
                }
            }
        }
        else -> null
    }
}