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

package cn.enaium.jimmer.buddy.database.model

/**
 * @author Enaium
 */
data class Table(
    val catalog: String,
    val schema: String,
    val name: String,
    var remark: String?,
    val columns: Set<Column>,
    val primaryKeys: Set<PrimaryKey>,
    val foreignKeys: MutableSet<ForeignKey>,
    val uniqueKeys: Set<UniqueKey>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Table

        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}