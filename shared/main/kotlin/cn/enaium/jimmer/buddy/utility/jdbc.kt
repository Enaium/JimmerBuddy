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

package cn.enaium.jimmer.buddy.utility

import cn.enaium.jimmer.buddy.database.model.*
import java.math.BigDecimal
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet

/**
 * @author Enaium
 */
internal enum class ColumnLabel {
    COLUMN_NAME, TABLE_NAME, TYPE_NAME, REMARKS, COLUMN_DEF, NULLABLE, PK_NAME, FKCOLUMN_NAME, PKTABLE_NAME, PKCOLUMN_NAME, FK_NAME, INDEX_NAME
}

private fun ResultSet.toColumn(tableName: String): Column {
    return Column(
        getString(ColumnLabel.COLUMN_NAME.name),
        tableName,
        getString(ColumnLabel.TYPE_NAME.name),
        getString(ColumnLabel.REMARKS.name),
        getString(ColumnLabel.COLUMN_DEF.name),
        getBoolean(ColumnLabel.NULLABLE.name)
    )
}

internal fun DatabaseMetaData.getTables(
    catalog: String? = null,
    schemaPattern: String? = null,
    tableNamePattern: String? = null
): Set<Table> {
    return getTables(catalog, schemaPattern, tableNamePattern, arrayOf("TABLE")).use { tableResult ->
        val tables = mutableSetOf<Table>()
        while (tableResult.next()) {
            val tableName = tableResult.getString(ColumnLabel.TABLE_NAME.name)
            val columns = getColumns(tableName)
            val primaryKeys = getPrimaryKeys(tableName)
            val foreignKeys = getForeignKeys(tableName)
            val uniqueKeys = getUniqueKeys(tableName)
            tables.add(Table(tableName, columns, primaryKeys, foreignKeys, uniqueKeys))
        }
        tables
    }
}

internal fun DatabaseMetaData.getColumns(tableName: String): Set<Column> {
    return getColumns(null, null, tableName, null).use { column ->
        val columns = mutableSetOf<Column>()
        while (column.next()) {
            columns.add(column.toColumn(tableName))
        }
        columns
    }
}

internal fun DatabaseMetaData.getPrimaryKeys(tableName: String): Set<PrimaryKey> {
    return getPrimaryKeys(null, null, tableName).use { primaryKey ->
        val primaryKeys = mutableSetOf<PrimaryKey>()
        while (primaryKey.next()) {
            val columnName = primaryKey.getString(ColumnLabel.COLUMN_NAME.name)
            val column = getColumns(tableName).first { it.name == columnName }
            primaryKeys.add(PrimaryKey(primaryKey.getString(ColumnLabel.PK_NAME.name), tableName, column))
        }
        primaryKeys
    }
}

internal fun DatabaseMetaData.getForeignKeys(tableName: String): MutableSet<ForeignKey> {
    return getImportedKeys(null, null, tableName).use { foreignKey ->
        val foreignKeys = mutableSetOf<ForeignKey>()
        while (foreignKey.next()) {
            val fkColumnName = foreignKey.getString(ColumnLabel.FKCOLUMN_NAME.name)
            val fkColumn = getColumns(tableName).first { it.name == fkColumnName }
            val pkTableName = foreignKey.getString(ColumnLabel.PKTABLE_NAME.name)
            val pkColumnName = foreignKey.getString(ColumnLabel.PKCOLUMN_NAME.name)
            val pkColumn = getColumns(pkTableName).first { it.name == pkColumnName }
            foreignKeys.add(ForeignKey(foreignKey.getString(ColumnLabel.FK_NAME.name), tableName, fkColumn, pkColumn))
        }
        foreignKeys
    }
}

internal fun DatabaseMetaData.getUniqueKeys(tableName: String): Set<UniqueKey> {
    val uniqueKey2Columns = mutableMapOf<String, CopyOnWriteArraySet<String>>()
    getIndexInfo(null, null, tableName, true, false).use { uniqueKey ->
        while (uniqueKey.next()) {
            val name = uniqueKey.getString(ColumnLabel.INDEX_NAME.name)
            val column = uniqueKey.getString(ColumnLabel.COLUMN_NAME.name)
            if (uniqueKey2Columns.containsKey(name)) {
                uniqueKey2Columns[name]!!.add(column)
            } else {
                uniqueKey2Columns[name] = CopyOnWriteArraySet(listOf(column))
            }
        }
    }
    return uniqueKey2Columns.map { (name, columns) ->
        UniqueKey(name, tableName, columns.map { getColumns(tableName).first { column -> column.name == it } })
    }.toSet()
}

internal val kotlinTypeMappings = mutableMapOf(
    "tinyint" to Byte::class.qualifiedName!!,
    "smallint" to Short::class.qualifiedName!!,
    "integer" to Int::class.qualifiedName!!,
    "bigint" to Long::class.qualifiedName!!,
    "decimal" to BigDecimal::class.qualifiedName!!,
    "numeric" to BigDecimal::class.qualifiedName!!,
    "varchar" to String::class.qualifiedName!!,
    "text" to String::class.qualifiedName!!,
    "date" to LocalDate::class.qualifiedName!!,
    "time" to LocalTime::class.qualifiedName!!,
    "datetime" to LocalDateTime::class.qualifiedName!!,
    "timestamp" to LocalDateTime::class.qualifiedName!!,
    "bool" to Boolean::class.qualifiedName!!,
    "boolean" to Boolean::class.qualifiedName!!,
    "uuid" to UUID::class.qualifiedName!!,
    "int2" to Short::class.qualifiedName!!,
    "int4" to Int::class.qualifiedName!!,
    "int8" to Long::class.qualifiedName!!,
    "float4" to Float::class.qualifiedName!!,
    "float8" to Double::class.qualifiedName!!,
)

internal val javaTypeMappings = mutableMapOf(
    "tinyint" to java.lang.Byte::class.java.name,
    "smallint" to java.lang.Short::class.java.name,
    "integer" to java.lang.Integer::class.java.name,
    "bigint" to java.lang.Long::class.java.name,
    "decimal" to BigDecimal::class.java.name,
    "numeric" to BigDecimal::class.java.name,
    "varchar" to java.lang.String::class.java.name,
    "text" to java.lang.String::class.java.name,
    "date" to LocalDate::class.java.name,
    "time" to LocalTime::class.java.name,
    "datetime" to LocalDateTime::class.java.name,
    "timestamp" to LocalDateTime::class.java.name,
    "bool" to java.lang.Boolean::class.java.name,
    "boolean" to java.lang.Boolean::class.java.name,
    "uuid" to UUID::class.java.name,
    "int2" to java.lang.Short::class.java.name,
    "int4" to java.lang.Integer::class.java.name,
    "int8" to java.lang.Long::class.java.name,
    "float4" to java.lang.Float::class.java.name,
    "float8" to java.lang.Double::class.java.name,
)