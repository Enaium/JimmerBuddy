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

package cn.enaium.jimmer.buddy.wizard

import cn.enaium.jimmer.buddy.template.JimmerProjectTemplateFile
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.GeneratorNewProjectWizardBuilderAdapter
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project
import com.intellij.util.io.createDirectories
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText

/**
 * @author Enaium
 */
class JimmerProjectBuilderAdapter(val jimmerWizard: JimmerProjectWizard = JimmerProjectWizard()) :
    GeneratorNewProjectWizardBuilderAdapter(jimmerWizard) {
    override fun createStep(context: WizardContext): NewProjectWizardStep {
        return jimmerWizard.createStep(context)
    }

    override fun createProject(name: String?, path: String?): Project? {
        return super.createProject(name, path)?.also {
            afterCreateProject(it)
        }
    }

    private val versionJimmer = mapOf(
        "name" to "jimmer",
        "version" to "0.9.61"
    )

    private val versionKotlin = mapOf(
        "name" to "kotlin",
        "version" to "2.1.10"
    )

    private val versionKsp = mapOf(
        "name" to "ksp",
        "version" to "2.1.10+"
    )

    private val libJimmerCore = mapOf(
        "name" to "jimmer-core",
        "module" to "org.babyfish.jimmer:jimmer-core",
        "versionRef" to "jimmer",
        "alias" to "jimmer.core"
    )

    private val libJimmerSql = mapOf(
        "name" to "jimmer-sql",
        "module" to "org.babyfish.jimmer:jimmer-sql",
        "versionRef" to "jimmer",
        "alias" to "jimmer.sql"
    )

    private val libJimmerSqlKotlin = mapOf(
        "name" to "jimmer-sql-kotlin",
        "module" to "org.babyfish.jimmer:jimmer-sql-kotlin",
        "versionRef" to "jimmer",
        "alias" to "jimmer.sql.kotlin"
    )

    private val libJimmerSpringBoot = mapOf(
        "name" to "jimmer-spring-boot",
        "module" to "org.babyfish.jimmer:jimmer-spring-boot-starter",
        "versionRef" to "jimmer",
        "alias" to "jimmer.spring.boot"
    )

    private val pluginKotlinJvm = mapOf(
        "name" to "kotlin-jvm",
        "id" to "org.jetbrains.kotlin.jvm",
        "versionRef" to "kotlin",
        "alias" to "kotlin.jvm"
    )

    private val pluginKsp = mapOf(
        "name" to "ksp",
        "id" to "com.google.devtools.ksp",
        "versionRef" to "ksp",
        "alias" to "ksp"
    )

    private fun afterCreateProject(project: Project) {
        val projectDir = Path(project.basePath!!)
        val projectModel = jimmerWizard.projectModel

        val srcDir = when (projectModel.language) {
            JimmerProjectModel.Language.KOTLIN -> projectDir.resolve("src/main/kotlin")
            JimmerProjectModel.Language.JAVA -> projectDir.resolve("src/main/java")
        }

        srcDir.createDirectories()

        val versions = listOf(
            versionJimmer
        ) + when (projectModel.language) {
            JimmerProjectModel.Language.KOTLIN -> {
                listOf(versionKotlin, versionKsp)
            }

            JimmerProjectModel.Language.JAVA -> {
                listOf()
            }
        }

        val libs = when (projectModel.type) {
            JimmerProjectModel.Type.SPRING_BOOT -> {
                listOf(libJimmerSpringBoot)
            }

            JimmerProjectModel.Type.SQL -> {
                when (projectModel.language) {
                    JimmerProjectModel.Language.KOTLIN -> {
                        listOf(libJimmerSqlKotlin)
                    }

                    JimmerProjectModel.Language.JAVA -> {
                        listOf(libJimmerSql)
                    }
                }
            }

            JimmerProjectModel.Type.IMMUTABLE -> {
                listOf(libJimmerCore)
            }
        }

        val plugins = when (projectModel.language) {
            JimmerProjectModel.Language.KOTLIN -> {
                listOf(pluginKotlinJvm, pluginKsp)
            }

            JimmerProjectModel.Language.JAVA -> {
                listOf()
            }
        }

        if (projectModel.builder == JimmerProjectModel.Builder.GRADLE) {
            val fileTemplateManager = FileTemplateManager.getInstance(project)
            val gradleWrapper = fileTemplateManager.getInternalTemplate(JimmerProjectTemplateFile.GRADLE_WRAPPER)
            val gradleWrapperContent = gradleWrapper.getText(mapOf("WRAPPER_VERSION" to projectModel.wrapperVersion))
            val gradleWrapperPath = projectDir.resolve("gradle/wrapper/gradle-wrapper.properties")
            if (gradleWrapperPath.exists().not()) {
                gradleWrapperPath.createParentDirectories()
            }
            val gradleToml = fileTemplateManager.getInternalTemplate(JimmerProjectTemplateFile.GRADLE_TOML)
            val gradleTomlContent = gradleToml.getText(
                mapOf(
                    "versions" to versions,
                    "libraries" to libs,
                    "plugins" to plugins
                )
            )
            val gradleTomlPath = projectDir.resolve("gradle/libs.versions.toml")
            gradleTomlPath.writeText(gradleTomlContent)
            val gradleBuild = fileTemplateManager.getInternalTemplate(JimmerProjectTemplateFile.GRADLE_BUILD)
            val gradleBuildContent = gradleBuild.getText(
                mapOf(
                    "GROUP" to projectModel.group,
                    "plugins" to plugins,
                    "libraries" to libs
                )
            )
            val gradleBuildPath = projectDir.resolve("build.gradle.kts")
            gradleBuildPath.writeText(gradleBuildContent)
            val gradleSettings = fileTemplateManager.getInternalTemplate(JimmerProjectTemplateFile.GRADLE_SETTINGS)
            val gradleSettingsContent = gradleSettings.getText(mapOf("ARTIFACT" to projectModel.artifact))
            val gradleSettingsPath = projectDir.resolve("settings.gradle.kts")
            gradleSettingsPath.writeText(gradleSettingsContent)
            gradleWrapperPath.writeText(gradleWrapperContent)
        }
    }
}