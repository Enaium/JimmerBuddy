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

import cn.enaium.jimmer.buddy.database.model.Table
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

/**
 * @author Enaium
 */
@Service(Service.Level.PROJECT)
@State(name = "JimmerBuddy.DatabaseCache", storages = [Storage("JimmerBuddy/DatabaseCache.xml")])
class DatabaseCache(val project: Project) : PersistentStateComponent<DatabaseCache.State> {
    val jackson = jacksonObjectMapper()

    override fun getState(): State {
        state.data = jackson.writeValueAsString(cache)
        return state
    }

    override fun loadState(p0: State) {
        state = p0
        cache = try {
            jackson.readValue<Cache>(p0.data)
        } catch (_: Throwable) {
            Cache()
        }
    }

    data class State(
        var data: String = "",
    )

    data class Cache(
        var databases: Set<DatabaseItem> = setOf(),
        var tables: Set<Table> = mutableSetOf(),
    )

    data class DatabaseItem(
        var uri: String,
        var username: String,
        var password: String,
        var catalog: String,
        var schemaPattern: String,
        var tableNamePattern: String,
        var driverFile: String,
        var driverName: String,
    )

    private var state = State()
    private var cache = Cache()

    companion object {
        fun getInstance(project: Project): Cache = project.getService(DatabaseCache::class.java).cache
    }
}