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
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

/**
 * @author Enaium
 */
class OracleDDLGenerate(project: Project, generateDDLModel: GenerateDDLModel) : DDLGenerate(project, generateDDLModel) {
    override fun typeMapping(type: String): String {
        return when (type) {
            String::class.java.name, String::class.java.name, String::class.qualifiedName -> "varchar2"
            Byte::class.java.name, Byte::class.java.name, Byte::class.qualifiedName -> "number(3)"
            Short::class.java.name, Short::class.java.name, Short::class.qualifiedName -> "number(5)"
            Int::class.java.name, Int::class.java.name, Int::class.qualifiedName -> "number(10)"
            Long::class.java.name, Long::class.java.name, Long::class.qualifiedName -> "number(19)"
            Float::class.java.name, Float::class.java.name, Float::class.qualifiedName -> "number(10, 5)"
            Double::class.java.name, Double::class.java.name, Double::class.qualifiedName -> "number(19, 5)"
            Boolean::class.java.name, Boolean::class.java.name, Boolean::class.qualifiedName -> "number(1)"
            BigDecimal::class.java.name -> "number(19, 5)"
            LocalDateTime::class.java.name, java.util.Date::class.java.name -> "timestamp"
            LocalDate::class.java.name -> "date"
            LocalTime::class.java.name -> "timestamp"
            UUID::class.java.name -> "varchar2(36)"
            else -> "varchar2"
        }
    }
}