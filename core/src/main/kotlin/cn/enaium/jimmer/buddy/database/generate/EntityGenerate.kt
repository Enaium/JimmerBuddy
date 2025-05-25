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

package cn.enaium.jimmer.buddy.database.generate

import cn.enaium.jimmer.buddy.database.model.Column
import cn.enaium.jimmer.buddy.database.model.GenerateEntityModel
import cn.enaium.jimmer.buddy.database.model.Table
import com.intellij.openapi.project.Project
import java.nio.file.Path

/**
 * @author Enaium
 */
interface EntityGenerate {
    fun generate(
        project: Project,
        generateEntity: GenerateEntityModel,
        tables: Set<Table>
    ): List<Path>

    fun getCommonColumns(tables: Set<Table>): Set<Column> {
        return tables.asSequence().flatMap { it.columns }.groupBy { it.name }
            .filter { it -> it.value.size == tables.count { it.primaryKeys.isNotEmpty() } }.map { it.value.first() }
            .toSet()
    }

    fun replaceName(tables: Set<Table>, generateEntity: GenerateEntityModel): Set<Table> {
        return tables.map { table ->
            table.copy(
                name = table.name.replace(Regex(generateEntity.tableNameRegex), generateEntity.tableNameReplace),
                columns = table.columns.map { column ->
                    column.copy(
                        name = column.name.replace(
                            Regex(generateEntity.columnNameRegex),
                            generateEntity.columnNameReplace
                        )
                    )
                }.toSet(),
                primaryKeys = table.primaryKeys.map { primaryKey ->
                    primaryKey.copy(
                        column = primaryKey.column.copy(
                            name = primaryKey.column.name.replace(
                                Regex(generateEntity.columnNameRegex),
                                generateEntity.columnNameReplace
                            )
                        )
                    )
                }.toSet(),
                foreignKeys = table.foreignKeys.map { foreignKey ->
                    foreignKey.copy(
                        column = foreignKey.column.copy(
                            name = foreignKey.column.name.replace(
                                Regex(generateEntity.columnNameRegex),
                                generateEntity.columnNameReplace
                            )
                        )
                    )
                }.toMutableSet(),
                uniqueKeys = table.uniqueKeys.map { uniqueKey ->
                    uniqueKey.copy(
                        columns = uniqueKey.columns.map { column ->
                            column.copy(
                                name = column.name.replace(
                                    Regex(generateEntity.columnNameRegex),
                                    generateEntity.columnNameReplace
                                )
                            )
                        }.toSet()
                    )
                }.toSet()
            )
        }.toSet()
    }
}

internal const val BASE_ENTITY = "BaseEntity"