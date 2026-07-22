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

import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoBody
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiDtoType
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoPsiFile
import cn.enaium.jimmer.buddy.extensions.dto.psi.DtoTypes
import cn.enaium.jimmer.buddy.utility.classLiteral
import cn.enaium.jimmer.buddy.utility.findProjectDir
import cn.enaium.jimmer.buddy.utility.firstCharLowercase
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.pom.Navigatable
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMember
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.psi.util.findParentOfType
import org.babyfish.jimmer.internal.GeneratedBy
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.core.util.toVirtualFile
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElementOfType
import java.util.*

/**
 * @author Enaium
 */
class GoToGeneratedByFile : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val dataContext = e.dataContext
        dataContext.getData(CommonDataKeys.PSI_ELEMENT)?.also { psiElement ->
            val element = when (psiElement) {
                is PsiMember -> {
                    var containingClass = psiElement.containingClass
                    while (containingClass?.findParentOfType<PsiClass>() != null) {
                        containingClass?.findParentOfType<PsiClass>()?.also {
                            containingClass = it
                        }
                    }
                    containingClass
                }

                is KtElement -> {
                    var containingClass = psiElement.containingClass()
                    while (containingClass?.findParentOfType<KtClass>() != null) {
                        containingClass?.findParentOfType<KtClass>()?.also {
                            containingClass = it
                        }
                    }
                    containingClass
                }

                else -> {
                    null
                }
            }



            ((element ?: psiElement).toUElementOfType<UClass>())?.also { uClass ->
                val generatedBy =
                    uClass.uAnnotations.find { it.qualifiedName == GeneratedBy::class.qualifiedName } ?: return@also
                generatedBy.findAttributeValue("file")?.evaluate()?.toString()?.also { file ->
                    val projectDir = findProjectDir(psiElement.containingFile.virtualFile.toNioPath()) ?: return@also
                    val dtoFile = projectDir.resolve(file.substringAfter("/"))
                    val psiFile =
                        PsiManager.getInstance(psiElement.project)
                            .findFile(dtoFile.toFile().toVirtualFile() ?: return@also)
                            ?: return@also

                    if (psiFile is DtoPsiFile) {
                        val dtoType =
                            PsiTreeUtil.findChildrenOfType(psiFile, DtoPsiDtoType::class.java).find { it.identifier.text == uClass.name }

                        when (psiElement) {
                            is PsiClass, is KtClass -> {
                                (dtoType?.identifier as? Navigatable)?.navigate(true)
                            }

                            is PsiMember -> {
                                var containingClass = psiElement.containingClass
                                val trace = mutableListOf(psiElement.name?.let {
                                    if (it.startsWith("get") || it.startsWith("set")) {
                                        it.substring(3).firstCharLowercase()
                                    } else {
                                        it
                                    }
                                } ?: return@also)
                                val prefix = "TargetOf_"
                                val className = containingClass?.name
                                if (className?.startsWith(prefix) == true) {
                                    trace.add(className.substring(prefix.length))
                                    while (containingClass?.findParentOfType<PsiClass>() != null) {
                                        containingClass.findParentOfType<PsiClass>()?.also {
                                            val className = it.name
                                            if (className?.startsWith(prefix) == true) {
                                                trace.add(className.substring(prefix.length))
                                            }
                                            containingClass = it
                                        }
                                    }
                                }
                                dtoType?.navigate(trace.reversed())
                            }

                            is KtParameter -> {
                                var containingClass = psiElement.containingClass()
                                val trace = mutableListOf(psiElement.name ?: return@also)
                                val prefix = "TargetOf_"
                                val className = containingClass?.name
                                if (className?.startsWith(prefix) == true) {
                                    trace.add(className.substring(prefix.length))
                                    while (containingClass?.findParentOfType<KtClass>() != null) {
                                        containingClass.findParentOfType<KtClass>()?.also {
                                            val className = it.name
                                            if (className?.startsWith(prefix) == true) {
                                                trace.add(className.substring(prefix.length))
                                            }
                                            containingClass = it
                                        }
                                    }
                                }
                                dtoType?.navigate(trace.reversed())
                            }
                        }
                    } else {
                        psiFile.navigate(true)
                    }
                }
                generatedBy.findAttributeValue("type")?.classLiteral()?.also { qualifiedName ->
                    (JavaPsiFacade.getInstance(psiElement.project)
                        .findClass(qualifiedName, psiElement.project.allScope())
                        ?: KotlinFullClassNameIndex[qualifiedName, psiElement.project, psiElement.project.allScope()].firstOrNull())?.also {
                        it.navigate(true)
                    }
                }
            }
        }
    }

    fun DtoPsiDtoType.navigate(trace: List<String>) {
        val trace = ArrayDeque(trace)
        var body: DtoPsiDtoBody? = dtoBody
        while (body != null) {
            trace.poll().also { poll ->
                val find = body.explicitPropList.find { explicitProp ->
                    val positiveProp = explicitProp.positiveProp
                    body = positiveProp?.propDtoBody?.dtoBody
                    positiveProp?.children?.firstOrNull { child ->
                        child.elementType == DtoTypes.IDENTIFIER
                    }?.text == poll
                }
                val positiveProp = find?.positiveProp
                if (positiveProp != null) {
                    val identifier = positiveProp.children.firstOrNull { it.elementType == DtoTypes.IDENTIFIER }
                    (identifier as? Navigatable)?.navigate(true)
                }
            } ?: break
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
            e.presentation.isVisible = ((element
                ?: psiElement).toUElementOfType<UClass>())?.uAnnotations?.find { it.qualifiedName == GeneratedBy::class.qualifiedName } != null
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}