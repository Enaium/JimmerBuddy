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

import cn.enaium.jimmer.buddy.database.generate.DDLGenerate
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.observable.properties.PropertyGraph

/**
 * @author Enaium
 */
class GenerateDDLModel : BaseState() {
    private val graph: PropertyGraph = PropertyGraph()
    val databaseProperty = graph.property(DDLGenerate.Database.POSTGRES)
    val existsStyleProperty = graph.property(DDLGenerate.ExistsStyle.CREATE)
    val referenceProperty = graph.property(true)
    val commentProperty = graph.property(false)
    val primaryKeyNameProperty = graph.property("id")

    val database: DDLGenerate.Database by databaseProperty
    val existsStyle: DDLGenerate.ExistsStyle by existsStyleProperty
    val reference: Boolean by referenceProperty
    val comment: Boolean by commentProperty
    val primaryKeyName: String by primaryKeyNameProperty
}