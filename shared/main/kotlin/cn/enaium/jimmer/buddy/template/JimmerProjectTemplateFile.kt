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

package cn.enaium.jimmer.buddy.template

import cn.enaium.jimmer.buddy.JimmerBuddy
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory

/**
 * @author Enaium
 */
class JimmerProjectTemplateFile : FileTemplateGroupDescriptorFactory {

    companion object {
        const val MAVEN_WRAPPER = "jimmer-maven-wrapper.properties"
        const val MAVEN_POM = "jimmer-maven-pom.xml"
        const val GRADLE_WRAPPER = "jimmer-gradle-wrapper.properties"
        const val GRADLE_BUILD = "jimmer-gradle-build.gradle.kts"
        const val GRADLE_SETTINGS = "jimmer-gradle-settings.gradle.kts"
        const val GRADLE_TOML = "jimmer-gradle-toml.toml"
        const val JIMMER_DTO_HEAD = "jimmer-dto-head.dto"
        const val JIMMER_DTO_CONTENT = "jimmer-dto-content.dto"
    }

    override fun getFileTemplatesDescriptor(): FileTemplateGroupDescriptor {
        val group = FileTemplateGroupDescriptor("Jimmer", JimmerBuddy.Icons.LOGO_NORMAL)
        group.addTemplate(MAVEN_WRAPPER)
        group.addTemplate(MAVEN_POM)
        group.addTemplate(GRADLE_WRAPPER)
        group.addTemplate(GRADLE_BUILD)
        group.addTemplate(GRADLE_SETTINGS)
        group.addTemplate(GRADLE_TOML)
        group.addTemplate(JIMMER_DTO_HEAD)
        group.addTemplate(JIMMER_DTO_CONTENT)
        return group
    }
}