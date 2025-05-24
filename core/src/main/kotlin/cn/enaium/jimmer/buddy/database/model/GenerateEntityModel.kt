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

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.observable.properties.PropertyGraph

/**
 * @author Enaium
 */
class GenerateEntityModel : BaseState() {
    private val graph: PropertyGraph = PropertyGraph()
    val relativePathProperty = graph.property<String>("")
    val packageNameProperty = graph.property<String>("")
    val languageProperty = graph.property<Language>(Language.KOTLIN)
    val commentProperty = graph.property<Boolean>(false)
    val tableAnnotationProperty = graph.property<Boolean>(false)
    val columnAnnotationProperty = graph.property<Boolean>(false)
    val idViewAnnotationProperty = graph.property<Boolean>(false)
    val joinTableAnnotationProperty = graph.property<Boolean>(false)
    val primaryKeyNameProperty = graph.property<String>("id")
    val associationProperty = graph.property<Association>(Association.REAL)

    val relativePath: String by relativePathProperty
    val packageName: String by packageNameProperty
    val language: Language by languageProperty
    val comment: Boolean by commentProperty
    val tableAnnotation: Boolean by tableAnnotationProperty
    val columnAnnotation: Boolean by columnAnnotationProperty
    val idViewAnnotation: Boolean by idViewAnnotationProperty
    val joinTableAnnotation: Boolean by joinTableAnnotationProperty
    val primaryKeyName: String by primaryKeyNameProperty
    val association: Association by associationProperty

    enum class Language(val text: String) {
        KOTLIN("Kotlin"),
        JAVA("Java"),
    }

    enum class Association(val text: String) {
        NO("No"),
        REAL("Real"),
        FAKE("Fake"),
    }
}