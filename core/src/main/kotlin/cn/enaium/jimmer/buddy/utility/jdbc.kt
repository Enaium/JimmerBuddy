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
import org.jetbrains.kotlin.idea.gradleTooling.get
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import java.util.concurrent.CopyOnWriteArraySet

/**
 * @author Enaium
 */
internal enum class ColumnLabel {
    COLUMN_NAME,
    TABLE_NAME,
    TYPE_NAME,
    REMARKS,
    COLUMN_DEF,
    NULLABLE,
    PK_NAME,
    FKCOLUMN_NAME,
    PKTABLE_NAME,
    PKCOLUMN_NAME,
    FK_NAME,
    INDEX_NAME,
    TABLE_CAT,
    TABLE_SCHEM
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
            val catalog = tableResult.getString(ColumnLabel.TABLE_CAT.name)
            val schema = tableResult.getString(ColumnLabel.TABLE_SCHEM.name)
            val remark = tableResult.getString(ColumnLabel.REMARKS.name)
            val columns = getColumns(tableName)
            val primaryKeys = getPrimaryKeys(tableName)
            val foreignKeys = getForeignKeys(tableName)
            val uniqueKeys = getUniqueKeys(tableName)
            tables.add(Table(catalog, schema, tableName, remark, columns, primaryKeys, foreignKeys, uniqueKeys))
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
        val primaryKeys = mutableMapOf<String, PrimaryKey>()
        while (primaryKey.next()) {
            val columnName = primaryKey.getString(ColumnLabel.COLUMN_NAME.name)
            val column = getColumns(tableName).first { it.name == columnName }
            val name = primaryKey.getString(ColumnLabel.PK_NAME.name)
            if (primaryKeys.contains(name)) {
                val pk = primaryKeys[name] ?: break
                primaryKeys[name] = pk.copy(columns = pk.columns + column)
            } else {
                primaryKeys[name] = PrimaryKey(name, tableName, setOf(column))
            }
        }
        primaryKeys.values.toSet()
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
        UniqueKey(name, tableName, columns.map { getColumns(tableName).first { column -> column.name == it } }.toSet())
    }.toSet()
}

internal enum class ColumnType {
    TINYINT,
    SMALLINT,
    INTEGER,
    BIGINT,
    DECIMAL,
    NUMERIC,
    VARCHAR,
    TEXT,
    DATE,
    TIME,
    DATETIME,
    TIMESTAMP,
    BOOL,
    BOOLEAN,
    UUID,
    INT2,
    INT4,
    INT8,
    FLOAT4,
    FLOAT8
}