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

package cn.enaium.jimmer.buddy.extensions.gradle.ksp

import org.gradle.api.Project
import org.jetbrains.plugins.gradle.tooling.AbstractModelBuilderService
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderContext
import kotlin.reflect.KProperty1

/**
 * @author Enaium
 */
class KspModelBuilderImpl : AbstractModelBuilderService() {
    override fun buildAll(
        modelName: String,
        project: Project,
        context: ModelBuilderContext
    ): Any? {
        if (!project.plugins.hasPlugin("com.google.devtools.ksp")) {
            return null
        }

        val ksp = project.extensions.getByName("ksp") ?: return null

        return KspModelImpl((ksp::class.members.find { it.name == "arguments" } as? KProperty1<Any, Map<String, String>>)?.get(
            ksp
        ) as Map<String, String>)
    }

    override fun canBuild(modelName: String): Boolean {
        return modelName == KspModel::class.java.name
    }

    override fun getErrorMessageBuilder(
        project: Project,
        e: Exception
    ): ErrorMessageBuilder {
        return ErrorMessageBuilder.create(project, e, "JimmerBuddy")
            .withDescription("Unable to build ksp configuration")
    }
}