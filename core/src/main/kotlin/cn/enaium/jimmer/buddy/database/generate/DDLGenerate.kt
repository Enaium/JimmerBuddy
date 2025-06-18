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

import cn.enaium.jimmer.buddy.database.model.*
import cn.enaium.jimmer.buddy.utility.*
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import org.jetbrains.kotlin.idea.base.util.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.KtClass

/**
 * @author Enaium
 */
abstract class DDLGenerate(val project: Project, val generateDDLModel: GenerateDDLModel) {
    fun tables(commonImmutableType: CommonImmutableType): List<Table> {
        val tables = mutableListOf<Table>()

        val tableName = commonImmutableType.name().camelToSnakeCase()

        val psiClass =
            JavaPsiFacade.getInstance(project).findClass(commonImmutableType.qualifiedName(), project.allScope())
        val ktClass =
            KotlinFullClassNameIndex[commonImmutableType.qualifiedName(), project, project.allScope()].firstOrNull() as? KtClass

        tables.add(
            Table(
                "",
                "",
                tableName,
                if (project.isJavaProject()) {
                    psiClass?.getComment()
                } else if (project.isKotlinProject()) {
                    ktClass?.getComment()
                } else {
                    null
                },
                commonImmutableType.props().mapNotNull { prop ->
                    if (!prop.isList()) {
                        prop.toColumn(tableName)
                            .copy(
                                remark = if (project.isJavaProject()) {
                                    psiClass?.methods?.find { it.name == prop.name() }?.getComment()
                                } else if (project.isKotlinProject()) {
                                    ktClass?.getProperties()?.find { it.name == prop.name() }?.getComment()
                                } else {
                                    null
                                }
                            )
                    } else {
                        null
                    }
                }.toSet(),
                commonImmutableType.props().mapNotNull { prop ->
                    prop.toPrimaryKey(tableName)
                }.toSet(),
                commonImmutableType.props().mapNotNull { prop ->
                    prop.toForeignKey(tableName)
                }.toMutableSet(),
                commonImmutableType.props().mapNotNull { prop ->
                    prop.toUniqueKey(tableName)
                }.toSet()
            )
        )
        return tables
    }

    fun CommonImmutableType.CommonImmutableProp.toColumn(tableName: String): Column {
        return Column(name().camelToSnakeCase().let {
            if (isAssociation(true) && !isList()) {
                "${it}_${generateDDLModel.primaryKeyName}"
            } else {
                it
            }
        }, tableName, typeName().replace("?", ""), null, null, isNullable())
    }

    fun CommonImmutableType.CommonImmutableProp.toPrimaryKey(tableName: String): PrimaryKey? {
        return if (isId()) {
            PrimaryKey(
                "pk_${tableName}_${name().camelToSnakeCase()}",
                tableName,
                setOf(toColumn(tableName))
            )
        } else {
            null
        }
    }

    fun CommonImmutableType.CommonImmutableProp.toForeignKey(tableName: String): ForeignKey? {
        return if (isAssociation(true) && !isList()) {
            ForeignKey(
                "fk_${tableName}_${name().camelToSnakeCase()}",
                tableName,
                toColumn(tableName),
                run {
                    val targetType = targetType() ?: return null
                    val targetTypeId = targetType.props().find { it.isId() } ?: return null
                    targetTypeId.toColumn(targetType.name().camelToSnakeCase())
                }
            )
        } else {
            null
        }
    }

    fun CommonImmutableType.CommonImmutableProp.toUniqueKey(tableName: String): UniqueKey? {
        return if (isKey()) {
            UniqueKey(
                "uk_${tableName}_${name().camelToSnakeCase()}",
                tableName,
                setOf(toColumn(tableName))
            )
        } else {
            null
        }
    }

    fun generate(commonImmutableType: CommonImmutableType): String {
        val tables = tables(commonImmutableType)

        fun Column.render(): String {
            var type = type

            tables.find { it.name == tableName }?.foreignKeys?.find { it.column == this }?.reference?.also {
                type = it.type
            }

            return "$name ${typeMapping(type)}".let { pk ->
                if (!nullable) {
                    " $pk not null"
                } else {
                    pk
                }
            }
        }

        return tables.joinToString("\n\n") { table ->
            var render = ""
            render += "create table ${table.name} (\n"
            render += table.columns.joinToString(",\n") { column ->
                "    ${column.render()}"
            }
            render += "\n);"

            if (table.primaryKeys.isNotEmpty()) {
                render += "\n"
                render += table.primaryKeys.joinToString("\n") { primaryKey ->
                    "alter table ${table.name} add constraint ${primaryKey.name} primary key (${primaryKey.columns.joinToString { it.name }});"
                }
            }

            if (table.uniqueKeys.isNotEmpty()) {
                render += "\n"
                render += table.uniqueKeys.joinToString("\n") { uniqueKey ->
                    "alter table ${table.name} add constraint ${uniqueKey.name} unique (${uniqueKey.columns.joinToString { it.name }});"
                }
            }

            if (generateDDLModel.reference) {
                render += "\n"
                render += table.foreignKeys.joinToString("\n") { foreignKey ->
                    "alter table ${table.name} add constraint ${foreignKey.name} foreign key (${foreignKey.column.name}) references ${foreignKey.reference.tableName} (${foreignKey.reference.name});"
                }
            }

            if (generateDDLModel.comment) {
                render += "\n"
                render += comment(tables)
            }

            render
        }
    }

    open fun comment(tables: List<Table>): String {
        var render = ""

        tables.forEach { table ->
            table.remark?.also {
                render += "comment on table ${table.name} is '${table.remark}'\n"
            }

            table.columns.forEach { column ->
                column.remark?.also {
                    render += "comment on column ${table.name}.${column.name} is '${column.remark}'\n"
                }
            }
        }

        return render
    }

    abstract fun typeMapping(type: String): String

    enum class Database(val text: String) {
        POSTGRES("Postgres"),
        MYSQL("MySQL"),
        MARIADB("MariaDB"),
        SQLITE("SQLite"),
        H2("H2"),
        ORACLE("Oracle"),
    }
}