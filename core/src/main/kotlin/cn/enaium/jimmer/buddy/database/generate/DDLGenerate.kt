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
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.PropOverride
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty

/**
 * @author Enaium
 */
abstract class DDLGenerate(val project: Project, val generateDDLModel: GenerateDDLModel) {
    fun tables(commonImmutableType: CommonImmutableType): List<Table> {
        val tables = mutableListOf<Table>()

        val tableName = commonImmutableType.name().camelToSnakeCase()

        tables.add(
            Table(
                "",
                "",
                tableName,
                commonImmutableType.psi(project)?.getComment(),
                commonImmutableType.props().mapNotNull { prop ->
                    if (!prop.isList() && !prop.isEmbedded()) {
                        prop.toColumn(tableName).copy(remark = prop.psi(project)?.getComment()).let {
                            val psi = prop.psi(project)
                            val name = when (psi) {
                                is PsiMethod -> {
                                    psi.modifierList.findAnnotation(org.babyfish.jimmer.sql.Column::class.qualifiedName!!)
                                        ?.findAttributeValue("name")?.toAny(String::class.java)?.toString()
                                }

                                is KtProperty -> {
                                    psi.findAnnotation(org.babyfish.jimmer.sql.Column::class.qualifiedName!!)
                                        ?.findArgument("name")?.value?.toString()
                                }

                                else -> {
                                    null
                                }
                            }

                            if (name != null) {
                                it.copy(name = name)
                            } else {
                                it
                            }
                        }
                    } else {
                        null
                    }
                }.toSet() + run {
                    val columns = mutableSetOf<Column>()

                    fun addEmbeddedProp(
                        type: CommonImmutableType,
                        map: MutableMap<String, String> = mutableMapOf()
                    ) {
                        type.props().forEach { prop ->
                            if (prop.isEmbedded()) {
                                prop.psi(project)?.also { psi ->
                                    if (psi is PsiMethod) {
                                        psi.annotations.forEach {
                                            if (it.qualifiedName == PropOverride::class.qualifiedName) {
                                                val prop =
                                                    it.findAttributeValue("prop")?.toAny(String::class.java)
                                                        ?.toString()
                                                val columnName = it.findAttributeValue("columnName")?.toAny(
                                                    String::class.java
                                                )?.toString()

                                                if (prop != null && columnName != null) {
                                                    map[prop] = columnName
                                                }
                                            }
                                        }
                                    } else if (psi is KtProperty) {
                                        psi.annotations().forEach {
                                            if (it.fqName == PropOverride::class.qualifiedName) {
                                                val prop = it.findArgument("prop")?.value?.toString()
                                                val columnName = it.findArgument("columnName")?.value?.toString()
                                                if (prop != null && columnName != null) {
                                                    map[prop] = columnName
                                                }
                                            }
                                        }
                                    }
                                }
                                prop.targetType()?.also {
                                    addEmbeddedProp(it, map)
                                    map.clear()
                                }
                            } else if (prop.declaringType().isEmbedded()) {
                                columns.add(prop.toColumn(tableName).let { column ->
                                    map[prop.name()]?.let {
                                        column.copy(it)
                                    } ?: column
                                })
                            }
                        }
                    }

                    addEmbeddedProp(commonImmutableType)

                    columns
                },
                commonImmutableType.props().mapNotNull { prop ->
                    prop.toPrimaryKey(tableName)
                }.toSet(),
                commonImmutableType.props().mapNotNull { prop ->
                    prop.toForeignKey(tableName)
                }.toMutableSet(),
                commonImmutableType.props().mapNotNull { prop ->
                    prop.toUniqueKey(tableName)
                }.groupBy { it.name to it.tableName }
                    .map { (k, v) -> UniqueKey(k.first, k.second, v.map { it.columns }.flatten().toSet()) }
                    .toSet()
            ).let { table ->
                val psi = commonImmutableType.psi(project)
                val name = when (psi) {
                    is PsiClass -> {
                        psi.modifierList?.findAnnotation(org.babyfish.jimmer.sql.Table::class.qualifiedName!!)
                            ?.findAttributeValue("name")
                            ?.toAny(String::class.java)?.toString()
                    }

                    is KtClass -> {
                        psi.findAnnotation(org.babyfish.jimmer.sql.Table::class.qualifiedName!!)
                            ?.findArgument("name")?.value?.toString()
                    }

                    else -> {
                        null
                    }
                }

                if (name != null) {
                    table.copy(name = name)
                } else {
                    table
                }
            }
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
            val psi = psi(project)

            val group = when (psi) {
                is PsiMethod -> {
                    psi.modifierList.findAnnotation(Key::class.qualifiedName!!)?.findAttributeValue("group")
                        ?.toAny(String::class.java).toString()
                }

                is KtProperty -> {
                    psi.annotations().find { it.fqName == Key::class.qualifiedName }
                        ?.findArgument("group")?.value?.toString()
                }

                else -> {
                    null
                }
            } ?: "default"

            UniqueKey(
                "uk_${tableName}_${group}",
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
                render += "comment on table ${table.name} is '${table.remark}';\n"
            }

            table.columns.forEach { column ->
                column.remark?.also {
                    render += "comment on column ${table.name}.${column.name} is '${column.remark}';\n"
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