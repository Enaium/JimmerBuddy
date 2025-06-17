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

import cn.enaium.jimmer.buddy.database.model.GenerateDDLModel
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * @author Enaium
 */
class MySqlDDLGenerate(generateDDLModel: GenerateDDLModel) : DDLGenerate(generateDDLModel) {
    override fun typeMapping(type: String): String {
        return when (type) {
            String::class.java.name, String::class.qualifiedName -> "varchar(255)"
            Byte::class.java.name, java.lang.Byte::class.qualifiedName, Byte::class.qualifiedName -> "tinyint"
            Short::class.java.name, java.lang.Short::class.qualifiedName, Short::class.qualifiedName -> "smallint"
            Int::class.java.name, java.lang.Integer::class.qualifiedName, Int::class.qualifiedName -> "int"
            Long::class.java.name, java.lang.Long::class.qualifiedName, Long::class.qualifiedName -> "bigint"
            Float::class.java.name, java.lang.Float::class.qualifiedName, Float::class.qualifiedName -> "float"
            Double::class.java.name, java.lang.Double::class.qualifiedName, Double::class.qualifiedName -> "double"
            Boolean::class.java.name, java.lang.Boolean::class.qualifiedName, Boolean::class.qualifiedName -> "bit"
            BigDecimal::class.qualifiedName -> "decimal"
            LocalDateTime::class.qualifiedName, java.util.Date::class.qualifiedName -> "datetime"
            java.time.LocalDate::class.qualifiedName -> "date"
            java.time.LocalTime::class.qualifiedName -> "time"
            java.util.Date::class.qualifiedName -> "datetime"
            java.util.UUID::class.qualifiedName -> "varchar(36)"
            else -> return "varchar(255)"
        }
    }
}