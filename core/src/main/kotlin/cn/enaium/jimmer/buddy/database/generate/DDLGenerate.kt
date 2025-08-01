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
import cn.enaium.jimmer.buddy.database.model.Column
import cn.enaium.jimmer.buddy.database.model.Table
import cn.enaium.jimmer.buddy.utility.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiMethod
import org.babyfish.jimmer.sql.*
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

/**
 * @author Enaium
 */
abstract class DDLGenerate(val project: Project, val generateDDLModel: GenerateDDLModel) {
    fun tables(commonImmutableType: CommonImmutableType): Set<Table> {
        val tables = mutableSetOf<Table>()

        val tableName = commonImmutableType.tableName()

        tables.add(
            Table(
                "",
                "",
                tableName,
                commonImmutableType.psi(project)?.getComment(),
                commonImmutableType.props().mapNotNull { prop ->
                    if (!prop.isList() && !prop.isEmbedded() && !prop.isFormula() && !prop.isTransient()) {
                        prop.toColumn(tableName)
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
            )
        )

        commonImmutableType.props().filter { it.isList() && it.isManyToMany() }.forEach { prop ->
            val psi = prop.psi(project)
            val selfName = commonImmutableType.tableName()
            val inverseName = prop.targetType()?.tableName() ?: return@forEach

            var joinName: String? = null
            var joinColumnName: String? = null
            var inverseJoinColumnName: String? = null

            when (psi) {
                is PsiMethod -> {
                    psi.modifierList.findAnnotation(ManyToMany::class.qualifiedName!!)?.findAttributeValue("mappedBy")
                        ?.toAny(String::class.java)?.toString()?.takeIf { it.isNotBlank() } != null && return@forEach
                    psi.modifierList.findAnnotation(JoinTable::class.qualifiedName!!)?.also {
                        joinName = it.findAttributeValue("name")?.toAny(String::class.java)?.toString()
                            ?.takeIf { it.isNotBlank() }
                        joinColumnName = it.findAttributeValue("joinColumnName")?.toAny(String::class.java)?.toString()
                            ?.takeIf { it.isNotBlank() }
                        inverseJoinColumnName =
                            it.findAttributeValue("inverseJoinColumnName")?.toAny(String::class.java)?.toString()
                                ?.takeIf { it.isNotBlank() }
                    }
                }

                is KtProperty -> {
                    val annotations = psi.annotations()
                    annotations.find { it.fqName == ManyToMany::class.qualifiedName!! }
                        ?.findArgument("mappedBy") != null && return@forEach
                    annotations.find { it.fqName == JoinTable::class.qualifiedName!! }?.also {
                        joinName = it.findArgument("name")?.value?.toString()
                        joinColumnName = it.findArgument("joinColumnName")?.value?.toString()
                        inverseJoinColumnName = it.findArgument("inverseJoinColumnName")?.value?.toString()
                    }
                }
            }

            val tableName = joinName ?: "${selfName}_${inverseName}_mapping"
            val type = commonImmutableType.props().find { it.isId() }?.typeName()?.replace("?", "") ?: return@forEach
            val self = Column(
                joinColumnName ?: "${selfName}_${generateDDLModel.primaryKeyName}",
                tableName,
                type,
                null,
                null,
                false
            )
            val inverse = Column(
                inverseJoinColumnName ?: "${inverseName}_${generateDDLModel.primaryKeyName}",
                tableName,
                type,
                null,
                null,
                false
            )
            tables.add(
                Table(
                    "",
                    "",
                    tableName,
                    "",
                    setOf(self, inverse),
                    emptySet(),
                    mutableSetOf(
                        ForeignKey(
                            "fk_${tableName}_${selfName}",
                            tableName,
                            self,
                            Column(generateDDLModel.primaryKeyName, selfName, type, null, null, false)
                        ),
                        ForeignKey(
                            "fk_${tableName}_${inverseName}",
                            tableName,
                            inverse,
                            Column(generateDDLModel.primaryKeyName, inverseName, type, null, null, false)
                        ),
                    ),
                    emptySet(),
                )
            )
        }

        return tables
    }

    fun CommonImmutableType.CommonImmutableProp.toColumn(tableName: String): Column {
        val psi = psi(project)
        return Column(
            columnName().let {
                if (isAssociation(true) && !isList()) {
                    "${it}_${generateDDLModel.primaryKeyName}"
                } else {
                    it
                }
            }, tableName, typeName().replace("?", ""), psi?.getComment(), when (psi) {
                is PsiMethod -> {
                    psi.modifierList.findAnnotation(Default::class.qualifiedName!!)?.findAttributeValue("value")
                        ?.toAny(String::class.java)?.toString()
                }

                is KtProperty -> {
                    psi.annotations().find { it.fqName == Default::class.qualifiedName!! }
                        ?.findArgument("value")?.value?.toString()
                }

                else -> {
                    null
                }
            }, isNullable()
        )
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

    open fun generate(commonImmutableType: CommonImmutableType): String {
        val tables = tables(commonImmutableType)

        fun Column.render(): String {
            var type = type

            tables.find { it.name == tableName }?.foreignKeys?.find { it.column == this }?.reference?.also {
                type = it.type
            }

            val typeMapping = typeMapping0(name, type)
            return "$name $typeMapping".let {
                if (defaultValue != null) {
                    val string = typeMapping.startsWith("text") || typeMapping.startsWith("varchar")
                    "$it default ${if (string) "'${defaultValue}'" else defaultValue}"
                } else {
                    it
                }.let { defaultValue ->
                    if (!nullable) {
                        "$defaultValue not null"
                    } else {
                        defaultValue
                    }
                }
            }
        }

        return tables.joinToString("\n") { table ->
            var render = ""
            if (generateDDLModel.existsStyle == ExistsStyle.DROP) {
                render += "drop table if exists ${table.name} cascade;\n"
            }
            render += "create table"
            if (generateDDLModel.existsStyle == ExistsStyle.CREATE) {
                render += " if not exists"
            }
            render += " ${table.name}\n"
            render += "(\n"
            render += table.columns.joinToString(",\n") { column ->
                "    ${column.render()}"
            }
            render += tableEnd(table)
            if (generateDDLModel.comment) {
                render += "\n"
                render += comment(tables)
            }
            render
        }
    }

    fun tableEnd(table: Table): String {
        var render = ""
        render += tableEndBefore(table)
        render += "\n);\n"
        render += tableEndAfter(table)
        return render
    }

    open fun tableEndBefore(table: Table): String {
        return ""
    }

    open fun tableEndAfter(table: Table): String {
        var render = ""
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
        return render
    }

    open fun comment(tables: Set<Table>): String {
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

    fun typeMapping0(column: String, type: String): String {
        return try {
            typeMapping(type)
        } catch (e: UnsupportedOperationException) {
            val fallback = e.message!!
            val textConstraints = mutableSetOf<String>()
            val numberConstraints = mutableSetOf<Int>()
            project.findPsiClass(type)?.takeIf { it.isEnum }?.also {
                it.getChildrenOfType<PsiEnumConstant>().forEach { enumContact ->
                    val enumItem = enumContact.modifierList?.findAnnotation(EnumItem::class.qualifiedName!!)
                    val value = enumItem?.findAttributeValue("name")?.toAny(String::class.java)?.toString()
                        ?.takeIf { value -> value.isNotBlank() }
                    val ordinal = enumItem?.findAttributeValue("ordinal")?.toAny(Int::class.java)?.toString()?.toInt()
                    if (value != null) {
                        textConstraints.add(value)
                    } else if (ordinal != null && ordinal != -892374651) {
                        numberConstraints.add(ordinal)
                    } else {
                        textConstraints.add(enumContact.name)
                    }
                }
            }
            project.findKtClass(type)?.takeIf { it.isEnum() }?.also {
                it.getChildOfType<KtClassBody>()?.getChildrenOfType<KtEnumEntry>()?.forEach { ktEnumEntry ->
                    val enumItem = ktEnumEntry.findAnnotation(EnumItem::class.qualifiedName!!)
                    val value = enumItem?.findArgument("name")?.value?.toString()
                    val ordinal = enumItem?.findArgument("ordinal")?.value?.toString()?.toInt()
                    if (value != null) {
                        textConstraints.add(value)
                    } else if (ordinal != null && ordinal != -892374651) {
                        numberConstraints.add(ordinal)
                    } else {
                        textConstraints.add(ktEnumEntry.name ?: return@forEach)
                    }
                }
            }

            fallback + if (textConstraints.isNotEmpty()) {
                " check (${column} in (${textConstraints.joinToString { "'${it}'" }}))"
            } else if (numberConstraints.isNotEmpty()) {
                " check (${column} in (${textConstraints.joinToString { it }}))"
            } else {
                ""
            }
        }
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

    enum class ExistsStyle(val text: String) {
        CREATE(I18n.message("dialog.generate.ddl.existsStyle.create")),
        DROP(I18n.message("dialog.generate.ddl.existsStyle.drop")),
    }

    private fun CommonImmutableType.tableName(): String {
        val psi = psi(project)
        return when (psi) {
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
        } ?: name().camelToSnakeCase()
    }

    private fun CommonImmutableType.CommonImmutableProp.columnName(): String {
        val psi = psi(project)
        return when (psi) {
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
        } ?: name().camelToSnakeCase()
    }
}