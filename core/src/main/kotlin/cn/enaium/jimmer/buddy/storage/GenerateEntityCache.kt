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

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

/**
 * @author Enaium
 */
@Service(Service.Level.PROJECT)
@State(name = "JimmerBuddy.GenerateEntityCache", storages = [Storage("JimmerBuddy/GenerateEntityCache.xml")])
class GenerateEntityCache : PersistentStateComponent<GenerateEntityCache.State> {
    private var state = State()

    data class State(
        var relativePath: String = "",
        var packageName: String = "",
    )

    override fun getState(): State {
        return state
    }

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(project: Project): State = project.getService(GenerateEntityCache::class.java).state
    }
}