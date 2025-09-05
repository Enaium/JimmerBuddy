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

package cn.enaium.jimmer.buddy.extensions.wizard

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.observable.properties.PropertyGraph

/**
 * @author Enaium
 */
class JimmerProjectModel : BaseState() {

    private val graph: PropertyGraph = PropertyGraph()
    val artifactProperty = graph.property("untitled")
    val groupProperty = graph.property("cn.example")
    val typeProperty = graph.property<Type>(Type.SPRING_BOOT)
    val languageProperty = graph.property<Language>(Language.KOTLIN)
    val builderProperty = graph.property<Builder>(Builder.GRADLE)
    val wrapperVersionProperty = graph.property("9.0.0")

    val artifact: String by artifactProperty
    val group: String by groupProperty
    val type: Type by typeProperty
    val language: Language by languageProperty
    val builder: Builder by builderProperty
    val wrapperVersion: String by wrapperVersionProperty

    enum class Type(val text: String) {
        SPRING_BOOT("Spring Boot"),
        SQL("SQL"),
        IMMUTABLE("Immutable"),
    }

    enum class Language(val text: String) {
        KOTLIN("Kotlin"),
        JAVA("Java"),
    }

    enum class Builder(val text: String) {
        GRADLE("Gradle"),
        MAVEN("Maven"),
    }
}