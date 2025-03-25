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

package cn.enaium.jimmer.buddy.storage

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import java.math.BigDecimal
import java.util.*

/**
 * @author Enaium
 */
@Service
@State(
    name = "JimmerBuddy",
    storages = [Storage("JimmerBuddy.xml")]
)
class JimmerBuddySetting : PersistentStateComponent<JimmerBuddySetting.Setting> {
    override fun getState(): Setting {
        return state
    }

    override fun loadState(p0: Setting) {
        state = p0
    }

    data class Setting(
        var databases: List<DatabaseItem> = listOf<DatabaseItem>(),
        var typeMapping: Map<String, JavaToKotlin> = mapOf(
            "tinyint" to JavaToKotlin(java.lang.Byte::class.java.name, Byte::class.qualifiedName!!),
            "smallint" to JavaToKotlin(
                java.lang.Short::class.java.name,
                Short::class.qualifiedName!!
            ),
            "integer" to JavaToKotlin(
                Integer::class.java.name,
                Int::class.qualifiedName!!
            ),
            "bigint" to JavaToKotlin(java.lang.Long::class.java.name, Long::class.qualifiedName!!),
            "decimal" to JavaToKotlin(
                BigDecimal::class.java.name,
                BigDecimal::class.qualifiedName!!
            ),
            "numeric" to JavaToKotlin(
                BigDecimal::class.java.name,
                BigDecimal::class.qualifiedName!!
            ),
            "varchar" to JavaToKotlin(
                java.lang.String::class.java.name,
                String::class.qualifiedName!!
            ),
            "text" to JavaToKotlin(java.lang.String::class.java.name, String::class.qualifiedName!!),
            "date" to JavaToKotlin(
                java.time.LocalDateTime::class.java.name,
                java.time.LocalDate::class.qualifiedName!!
            ),
            "time" to JavaToKotlin(
                java.time.LocalTime::class.java.name,
                java.time.LocalTime::class.qualifiedName!!
            ),
            "datetime" to JavaToKotlin(
                java.time.LocalDateTime::class.java.name,
                java.time.LocalDateTime::class.qualifiedName!!
            ),
            "timestamp" to JavaToKotlin(
                java.time.LocalDateTime::class.java.name,
                java.time.LocalDateTime::class.qualifiedName!!
            ),
            "bool" to JavaToKotlin(
                java.lang.Boolean::class.java.name,
                Boolean::class.qualifiedName!!
            ),
            "boolean" to JavaToKotlin(
                java.lang.Boolean::class.java.name,
                Boolean::class.qualifiedName!!
            ),
            "uuid" to JavaToKotlin(UUID::class.java.name, UUID::class.qualifiedName!!),
            "int2" to JavaToKotlin(java.lang.Short::class.java.name, Short::class.qualifiedName!!),
            "int4" to JavaToKotlin(Integer::class.java.name, Int::class.qualifiedName!!),
            "int8" to JavaToKotlin(java.lang.Long::class.java.name, Long::class.qualifiedName!!),
            "float4" to JavaToKotlin(java.lang.Float::class.java.name, Float::class.qualifiedName!!),
            "float8" to JavaToKotlin(
                java.lang.Double::class.java.name,
                Double::class.qualifiedName!!
            )
        )
    )

    data class DatabaseItem(
        var uri: String = "",
        var username: String = "",
        var password: String = "",
        var catalog: String = "",
        var schemaPattern: String = "",
        var tableNamePattern: String = ""
    )

    data class JavaToKotlin(
        var javaType: String = "",
        var kotlinType: String = ""
    )

    private var state = Setting()

    companion object {
        val INSTANCE: JimmerBuddySetting
            get() = ApplicationManager.getApplication().getService(JimmerBuddySetting::class.java)
    }
}