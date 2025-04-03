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

package cn.enaium.jimmer.buddy.extensions

import cn.enaium.jimmer.buddy.JimmerBuddy
import cn.enaium.jimmer.buddy.storage.JimmerBuddySetting
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.options.Configurable
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/**
 * @author Enaium
 */
class BuddySettingUI : Configurable {
    override fun getDisplayName(): String {
        return JimmerBuddy.NAME
    }

    val model = Model().also {
        it.logo = JimmerBuddySetting.INSTANCE.state.logo
    }

    override fun createComponent(): JComponent {
        return panel {
            row {
                checkBox("Logo").bindSelected(model.logoProperty)
            }
        }
    }

    override fun isModified(): Boolean {
        return model.logo != JimmerBuddySetting.INSTANCE.state.logo
    }

    override fun apply() {
        JimmerBuddySetting.INSTANCE.state.logo = model.logo
    }

    class Model : BaseState() {
        private val graph: PropertyGraph = PropertyGraph()
        val logoProperty = graph.property<Boolean>(true)
        var logo by logoProperty
    }
}