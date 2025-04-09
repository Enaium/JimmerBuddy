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

import cn.enaium.jimmer.buddy.utility.getAllProperties
import cn.enaium.jimmer.buddy.utility.getTarget
import cn.enaium.jimmer.buddy.utility.isImmutable
import cn.enaium.jimmer.buddy.utility.string
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import org.babyfish.jimmer.sql.ManyToMany
import org.babyfish.jimmer.sql.OneToMany
import org.babyfish.jimmer.sql.OrderedProp
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.toUElementOfType

/**
 * @author Enaium
 */
class OrderedPropAnnotationInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element !is PsiAnnotation && element !is KtAnnotationEntry && element.getParentOfType<PsiClass>(true)
                        ?.isImmutable() != true && element.getParentOfType<KtClass>(true)?.isImmutable() != true
                ) {
                    return
                }

                if (element is PsiAnnotation && listOf(
                        OneToMany::class.qualifiedName,
                        ManyToMany::class.qualifiedName
                    ).contains(element.getParentOfType<PsiAnnotation>(true)?.qualifiedName)
                    && element.qualifiedName == OrderedProp::class.qualifiedName
                ) {
                    val name = element.toUElementOfType<UAnnotation>()?.findAttributeValue("value")?.string() ?: return
                    element.getParentOfType<PsiMethod>(true)?.getTarget()?.allMethods?.find { it.name == name }
                        ?: holder.registerProblem(element, "The property is not found")
                }

                if (listOf(
                        OneToMany::class.qualifiedName,
                        ManyToMany::class.qualifiedName
                    ).contains(
                        element.getParentOfType<KtAnnotationEntry>(true)?.toUElementOfType<UAnnotation>()?.qualifiedName
                    )
                    && element.toUElementOfType<UAnnotation>()?.qualifiedName == OrderedProp::class.qualifiedName
                ) {
                    val name = element.toUElementOfType<UAnnotation>()?.findAttributeValue("value")?.string() ?: return
                    element.getParentOfType<KtProperty>(true)?.getTarget()?.getAllProperties()?.find { it.name == name }
                        ?: holder.registerProblem(element, "The property is not found")
                }
            }
        }
    }
}