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
    val relativePathProperty = graph.property("")
    val packageNameProperty = graph.property("")
    val languageProperty = graph.property<Language>(Language.KOTLIN)
    val commentProperty = graph.property(false)
    val tableAnnotationProperty = graph.property(false)
    val columnAnnotationProperty = graph.property(false)
    val idViewAnnotationProperty = graph.property(false)
    val joinTableAnnotationProperty = graph.property(false)
    val primaryKeyNameProperty = graph.property("id")
    val associationProperty = graph.property<Association>(Association.REAL)
    val tableNameRegexProperty = graph.property("")
    val tableNameReplaceProperty = graph.property("")
    val columnNameRegexProperty = graph.property("")
    val columnNameReplaceProperty = graph.property("")

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
    val tableNameRegex: String by tableNameRegexProperty
    val tableNameReplace: String by tableNameReplaceProperty
    val columnNameRegex: String by columnNameRegexProperty
    val columnNameReplace: String by columnNameReplaceProperty

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