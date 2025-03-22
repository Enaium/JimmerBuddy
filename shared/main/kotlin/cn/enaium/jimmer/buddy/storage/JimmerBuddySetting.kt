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
        var databases: List<DatabaseItem> = listOf<DatabaseItem>()
    )

    data class DatabaseItem(
        val uri: String,
        val username: String,
        val password: String
    )

    private var state = Setting()

    companion object {
        val INSTANCE: JimmerBuddySetting
            get() = ApplicationManager.getApplication().getService(JimmerBuddySetting::class.java)
    }
}