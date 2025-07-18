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
import com.intellij.openapi.project.Project
import java.time.LocalDateTime
import java.util.*

/**
 * @author Enaium
 */
class MariadbGenerate(project: Project, generateDDLModel: GenerateDDLModel) :
    MySqlDDLGenerate(project, generateDDLModel) {
    override fun typeMapping(type: String): String {
        return when (type) {
            String::class.java.name, String::class.java.name, String::class.qualifiedName -> "varchar(255)"
            Byte::class.java.name, java.lang.Byte::class.java.name, Byte::class.qualifiedName -> "tinyint"
            Short::class.java.name, java.lang.Short::class.java.name, Short::class.qualifiedName -> "smallint"
            Int::class.java.name, java.lang.Integer::class.java.name, Int::class.qualifiedName -> "int"
            Long::class.java.name, java.lang.Long::class.java.name, Long::class.qualifiedName -> "bigint"
            Float::class.java.name, java.lang.Float::class.java.name, Float::class.qualifiedName -> "float"
            Double::class.java.name, java.lang.Double::class.java.name, Double::class.qualifiedName -> "double"
            Boolean::class.java.name, java.lang.Boolean::class.java.name, Boolean::class.qualifiedName -> "bool"
            LocalDateTime::class.java.name, java.util.Date::class.java.name -> "datetime"
            java.time.LocalDate::class.java.name -> "date"
            java.time.LocalTime::class.java.name -> "time"
            java.util.Date::class.java.name -> "datetime"
            UUID::class.java.name -> "uuid"
            else -> throw UnsupportedOperationException("varchar(255)")
        }
    }
}