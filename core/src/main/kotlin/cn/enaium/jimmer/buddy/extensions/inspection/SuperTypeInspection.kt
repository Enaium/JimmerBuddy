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

package cn.enaium.jimmer.buddy.extensions.inspection

import cn.enaium.jimmer.buddy.utility.annotations
import cn.enaium.jimmer.buddy.utility.isImmutable
import cn.enaium.jimmer.buddy.utility.type
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiReferenceList
import org.babyfish.jimmer.sql.MappedSuperclass
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

/**
 * @author Enaium
 */
class SuperTypeInspection : AbstractLocalInspectionTool() {
    override fun visit(
        element: PsiElement,
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ) {
        val descriptionTemplate = "Super type must be annotated with @MappedSuperclass"
        if (element is PsiClass && element.isImmutable()) {
            element.getChildrenOfType<PsiReferenceList>().forEach { psiReferenceList ->
                psiReferenceList.getChildrenOfType<PsiJavaCodeReferenceElement>()
                    .forEach { psiJavaCodeReferenceElement ->
                        if ((psiJavaCodeReferenceElement.resolve() as? PsiClass)?.annotations?.any { annotation -> annotation.qualifiedName == MappedSuperclass::class.qualifiedName } == false) {
                            holder.registerProblem(psiJavaCodeReferenceElement, descriptionTemplate)
                        }
                    }
            }
        } else if (element is KtClass && element.isImmutable()) {
            element.superTypeListEntries.forEach { superTypeListEntry ->
                superTypeListEntry.typeReference?.type()?.ktClass?.annotations()
                    ?.find { annotation -> annotation.fqName == MappedSuperclass::class.qualifiedName }
                    ?: run {
                        holder.registerProblem(superTypeListEntry, descriptionTemplate)
                    }
            }
        }
    }
}