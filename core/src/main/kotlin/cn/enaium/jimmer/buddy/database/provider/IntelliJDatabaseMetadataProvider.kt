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

package cn.enaium.jimmer.buddy.database.provider

import cn.enaium.jimmer.buddy.database.model.*
import cn.enaium.jimmer.buddy.storage.DatabaseCache
import com.intellij.database.model.*
import com.intellij.database.psi.DbPsiFacade
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project

/**
 * Provider that uses com.intellij.database API to retrieve database metadata.
 * Falls back to [JdbcDatabaseMetadataProvider] when the plugin is not available or the API call fails.
 *
 * @author Enaium
 */
class IntelliJDatabaseMetadataProvider : DatabaseMetadataProvider {

    override fun getTables(project: Project, item: DatabaseCache.DatabaseItem): Set<Table> {
        if (!isDatabasePluginAvailable()) {
            return JdbcDatabaseMetadataProvider().getTables(project, item)
        }

        val intellijDataSourceId = item.intellijDataSourceId
        if (intellijDataSourceId.isBlank()) {
            return JdbcDatabaseMetadataProvider().getTables(project, item)
        }

        return try {
            getTablesFromIntelliJApi(project, intellijDataSourceId)
                ?: JdbcDatabaseMetadataProvider().getTables(project, item)
        } catch (e: Exception) {
            JdbcDatabaseMetadataProvider().getTables(project, item)
        }
    }

    private fun getTablesFromIntelliJApi(project: Project, dataSourceId: String): Set<Table>? {
        val facade = DbPsiFacade.getInstance(project)
        val dbDataSource = facade.dataSources.find { it.uniqueId == dataSourceId } ?: return null

        val model = dbDataSource.model
        val tables = model.traverser()
            .filter(DasTable::class.java)
            .toList()

        return tables.mapNotNull { table ->
            try {
                val dasTable = table as DasTable
                extractTable(dasTable)
            } catch (_: Exception) {
                null
            }
        }.toSet()
    }

    private fun extractTable(dasTable: DasTable): Table? {
        return try {
            val tableName = dasTable.name

            val parent = dasTable.dasParent
            val schemaName = if (parent is DasNamespace) {
                parent.name ?: ""
            } else ""

            val remark = dasTable.comment ?: ""

            val columns = dasTable.getDasChildren(ObjectKind.COLUMN)
                .filterIsInstance<DasColumn>()
                .mapNotNull { col -> extractColumn(col, tableName) }
                .toSet()

            val primaryKeys = extractPrimaryKeys(dasTable, columns)
            val foreignKeys = extractForeignKeys(dasTable, columns)
            val uniqueKeys = extractUniqueKeys(dasTable, columns)

            Table("", schemaName, tableName, remark, columns, primaryKeys, foreignKeys.toMutableSet(), uniqueKeys)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractColumn(col: DasColumn, tableName: String): Column? {
        return try {
            val name = col.name
            val typeName = col.dasType.description
            val defaultValue = col.default
            val nullable = !col.isNotNull
            val remark = col.comment ?: ""
            Column(name, tableName, typeName, remark, defaultValue, nullable)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractPrimaryKeys(table: DasTable, columns: Set<Column>): Set<PrimaryKey> {
        val keys = table.getDasChildren(ObjectKind.KEY)
        return keys.mapNotNull { keyObj ->
            try {
                val constraint = keyObj as? DasConstraint ?: return@mapNotNull null
                val pkName = constraint.name
                val pkColumns = constraint.columnsRef.resolveObjects().mapNotNull { ref ->
                    columns.find { it.name == ref.name }
                }.toSet()
                PrimaryKey(pkName, "", pkColumns)
            } catch (_: Exception) {
                null
            }
        }.toSet()
    }

    private fun extractForeignKeys(table: DasTable, columns: Set<Column>): Set<ForeignKey> {
        val fkElements = table.getDasChildren(ObjectKind.FOREIGN_KEY)
        return fkElements.mapNotNull { fkObj ->
            try {
                val fk = fkObj as? DasForeignKey ?: return@mapNotNull null
                val fkName = fk.name
                val refTableName = fk.refTable?.name ?: ""

                val fkColumnNames = fk.columnsRef.resolveObjects().map { it.name }.toList()
                val refColumnNames = fk.refColumns.resolveObjects().map { it.name }.toList()

                fkColumnNames.zip(refColumnNames).firstOrNull()?.let { (fkColName, refColName) ->
                    val fkModel = columns.find { it.name == fkColName }
                    val refModel = Column(refColName, refTableName, "", "", null, false)
                    if (fkModel != null) {
                        ForeignKey(fkName, "", fkModel, refModel)
                    } else null
                }
            } catch (_: Exception) {
                null
            }
        }.toSet()
    }

    private fun extractUniqueKeys(table: DasTable, columns: Set<Column>): Set<UniqueKey> {
        val indexElements = table.getDasChildren(ObjectKind.INDEX)
        return indexElements.mapNotNull { indexObj ->
            try {
                val index = indexObj as? DasIndex ?: return@mapNotNull null
                if (!index.isUnique) return@mapNotNull null
                val indexName = index.name
                val indexColumns = index.columnsRef.resolveObjects().mapNotNull { ref ->
                    columns.find { it.name == ref.name }
                }.toSet()
                UniqueKey(indexName, "", indexColumns)
            } catch (_: Exception) {
                null
            }
        }.toSet()
    }

    /**
     * Converts a single [DasTable] object to a [Table] model.
     * This is used by [GenerateFromDbTableAction] to extract table metadata from the
     * Database Tools tree selection.
     */
    fun extractTableFromDasTable(dasTable: DasTable): Table? {
        return extractTable(dasTable)
    }

    fun getDataSources(project: Project): List<IntelliJDataSourceInfo> {
        if (!isDatabasePluginAvailable()) return emptyList()

        return try {
            val facade = DbPsiFacade.getInstance(project)
            facade.dataSources.mapNotNull { ds ->
                try {
                    IntelliJDataSourceInfo(
                        id = ds.uniqueId,
                        name = ds.name,
                        url = ds.connectionConfig?.url ?: "",
                        username = "",
                        driverClass = ds.connectionConfig?.driverClass ?: ""
                    )
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun findDataSource(project: Project, id: String): IntelliJDataSourceInfo? {
        return getDataSources(project).find { it.id == id }
    }

    /**
     * Returns all tables from all IntelliJ database data sources.
     * Used by [TableCompletionProvider] to provide table name completion
     * without relying on the persistent [DatabaseCache].
     */
    fun getAllTables(project: Project): Set<Table> {
        if (!isDatabasePluginAvailable()) return emptySet()

        return try {
            val facade = DbPsiFacade.getInstance(project)
            facade.dataSources.flatMap { ds ->
                try {
                    getTablesFromIntelliJApi(project, ds.uniqueId) ?: emptySet()
                } catch (_: Exception) {
                    emptySet()
                }
            }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    companion object {
        fun isDatabasePluginAvailable(): Boolean {
            return PluginManagerCore.isPluginInstalled(PluginId.getId("com.intellij.database"))
        }
    }
}