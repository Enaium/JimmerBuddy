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

import cn.enaium.jimmer.buddy.utility.I18n
import cn.enaium.jimmer.buddy.utility.annotations
import cn.enaium.jimmer.buddy.utility.getAllProperties
import cn.enaium.jimmer.buddy.utility.isImmutable
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.jetbrains.kotlin.psi.KtClass

/**
 * @author Enaium
 */
class IdAnnotationInspection : AbstractLocalInspectionTool() {
    override fun visit(
        element: PsiElement,
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ) {
        val descriptionTemplate = I18n.message("inspection.annotation.id.notHaveId")
        if (element is PsiClass && element.isImmutable() && element.annotations
                .any { it.qualifiedName == Entity::class.qualifiedName }
        ) {
            if (!element.allMethods.any { method -> method.annotations.any { it.qualifiedName == Id::class.qualifiedName } }) {
                holder.registerProblem(element.nameIdentifier ?: return, descriptionTemplate)
            }
        } else if (element is KtClass && element.isImmutable() && element.annotations()
                .any { it.fqName == Entity::class.qualifiedName }
        ) {
            if (!element.getAllProperties()
                    .any { property -> property.annotations().any { it.fqName == Id::class.qualifiedName } }
            ) {
                holder.registerProblem(element.nameIdentifier ?: return, descriptionTemplate)
            }
        }
    }
}