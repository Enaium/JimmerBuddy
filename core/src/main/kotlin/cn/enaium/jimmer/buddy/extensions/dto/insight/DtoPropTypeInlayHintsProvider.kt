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

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiNamedElement
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiProp
import cn.enaium.jimmer.buddy.utility.endOffset
import cn.enaium.jimmer.buddy.utility.firstCharUppercase
import cn.enaium.jimmer.buddy.utility.resolveClass
import cn.enaium.jimmer.buddy.utility.resolveGenericsClassInType
import cn.enaium.jimmer.buddy.utility.type
import com.intellij.codeInsight.hints.declarative.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer

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
                if (element !is DtoPsiProp && element !is DtoPsiNamedElement) return
                val reference = (element as DtoPsiNamedElement).reference() ?: return
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