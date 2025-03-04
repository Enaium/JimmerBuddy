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
import com.jetbrains.rd.util.reflection.toPath
import java.net.URI
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager

/**
 * @author Enaium
 */
interface EntityGenerate {
    fun generate(projectDir: Path, generateEntity: GenerateEntityModel)

    fun getConnection(generateEntity: GenerateEntityModel): Connection {
        return URI.create(generateEntity.uri).takeIf { it.scheme == "file" }?.let { ddl ->
            DriverManager.getConnection(
                "jdbc:h2:mem:test;DATABASE_TO_LOWER=true;INIT=RUNSCRIPT FROM '${
                    ddl.toURL().toPath().absolutePath.replace(
                        "\\",
                        "/"
                    )
                }'"
            )
        } ?: let {
            DriverManager.getConnection(
                generateEntity.uri,
                generateEntity.username,
                generateEntity.password
            )
        }
    }

    fun getCommonColumns(tables: Set<Table>): Set<Column> {
        return tables.asSequence().flatMap { it.columns }.groupBy { it.name }
            .filter { it -> it.value.size == tables.count { it.primaryKeys.isNotEmpty() } }.map { it.value.first() }
            .toSet()
    }
}

internal const val BASE_ENTITY = "BaseEntity"