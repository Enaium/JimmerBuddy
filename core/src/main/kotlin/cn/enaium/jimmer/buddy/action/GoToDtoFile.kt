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

package cn.enaium.jimmer.buddy.action

import cn.enaium.jimmer.buddy.utility.findProjectDir
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMember
import org.babyfish.jimmer.internal.GeneratedBy
import org.jetbrains.kotlin.idea.core.util.toVirtualFile
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElementOfType

/**
 * @author Enaium
 */
class GoToDtoFile : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val dataContext = e.dataContext
        dataContext.getData(CommonDataKeys.PSI_ELEMENT)?.also { psiElement ->

            val element = when (psiElement) {
                is PsiMember -> {
                    psiElement.containingClass
                }

                is KtElement -> {
                    psiElement.containingClass()
                }

                else -> {
                    null
                }
            }

            ((element ?: psiElement).toUElementOfType<UClass>())?.also { uClass ->
                val generatedBy =
                    uClass.uAnnotations.find { it.qualifiedName == GeneratedBy::class.qualifiedName } ?: return@also
                val file = generatedBy.findAttributeValue("file")?.evaluate()?.toString() ?: return@also
                val projectDir = findProjectDir(psiElement.containingFile.virtualFile.toNioPath()) ?: return@also
                val dtoFile = projectDir.resolve(file.substringAfter("/"))
                val psiFile =
                    PsiManager.getInstance(psiElement.project).findFile(dtoFile.toFile().toVirtualFile() ?: return@also)
                        ?: return@also
                psiFile.navigate(true)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = false
        val dataContext = e.dataContext
        dataContext.getData(CommonDataKeys.PSI_ELEMENT)?.also { psiElement ->
            val element = when (psiElement) {
                is PsiMember -> {
                    psiElement.containingClass
                }

                is KtElement -> {
                    psiElement.containingClass()
                }

                else -> {
                    null
                }
            }
            ((element ?: psiElement)
                .toUElementOfType<UClass>())?.uAnnotations?.find { it.qualifiedName == GeneratedBy::class.qualifiedName }
                ?.also { generated ->
                    e.presentation.isVisible = generated.findAttributeValue("file")?.toString()?.isNotBlank() == true
                }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}