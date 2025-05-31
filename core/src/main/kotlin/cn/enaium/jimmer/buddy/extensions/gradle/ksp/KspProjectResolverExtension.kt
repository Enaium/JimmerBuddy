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

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

/**
 * @author Enaium
 */
class KspProjectResolverExtension : AbstractProjectResolverExtension() {
    override fun getExtraProjectModelClasses(): Set<Class<*>> {
        return setOf(KspModel::class.java)
    }

    override fun getToolingExtensionsClasses(): Set<Class<*>> {
        return extraProjectModelClasses
    }

    override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        val kspModel = resolverCtx.getExtraProject(gradleModule, KspModel::class.java)
        kspModel?.also {
            val data = KspData(ideModule.data, kspModel.arguments)
            ideModule.createChild(KspData.KEY, data)
        }
        super.populateModuleExtraModels(gradleModule, ideModule)
    }
}