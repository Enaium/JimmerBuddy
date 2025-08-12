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

import cn.enaium.jimmer.buddy.storage.DatabaseCache
import cn.enaium.jimmer.buddy.utility.*
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.findParentOfType
import org.babyfish.jimmer.sql.Column
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElementOfType

/**
 * @author Enaium
 */
class EntityDatabaseInspection : AbstractLocalInspectionTool() {
    override fun visit(
        element: PsiElement,
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ) {
        val tables = DatabaseCache.getInstance(element.project).tables
        tables.isEmpty() && return
        if (element is PsiClass || element is KtClass) {
            val uClass = element.toUElementOfType<UClass>() ?: return
            uClass.isEntity().not() && return
            val table = uClass.getTableName()
            if (tables.find { it.name == table } == null) {
                element.nameIdentifier?.also {
                    holder.registerProblem(it, I18n.message("inspection.entityDatabase.tableDoesNotExist", table))
                }
            }
        }

        if (element is PsiMethod || element is KtProperty) {
            val uClass =
                element.findParentOfType<PsiClass>()?.toUElementOfType<UClass>() ?: element.findParentOfType<KtClass>()
                    ?.toUElementOfType<UClass>() ?: return
            uClass.isEntity().not() && return
            val table = uClass.getTableName()

            val column = when (element) {
                is PsiMethod -> {
                    (element.hasToOneAnnotation() || element.hasToManyAnnotation() || element.isComputed() || element.hasSerializedAnnotation()) && return

                    element.annotations.find { annotation -> annotation.hasQualifiedName(Column::class.qualifiedName!!) }
                        ?.findAttributeValue("name")?.toAny(String::class.java)?.toString()
                }

                is KtProperty -> {
                    (element.hasToOneAnnotation() || element.hasToManyAnnotation() || element.isComputed() || element.hasSerializedAnnotation()) && return

                    element.annotations().find { annotation -> annotation.fqName == Column::class.qualifiedName }
                        ?.findArgument("name")?.value?.toString()
                }

                else -> {
                    null
                }
            } ?: element.nameIdentifier?.text?.camelToSnakeCase()

            if (tables.find { it.name == table }?.columns?.find { it.name == column } == null) {
                element.nameIdentifier?.also {
                    holder.registerProblem(
                        it,
                        I18n.message("inspection.entityDatabase.columnDoesNotExist", column, table)
                    )
                }
            }
        }
    }
}