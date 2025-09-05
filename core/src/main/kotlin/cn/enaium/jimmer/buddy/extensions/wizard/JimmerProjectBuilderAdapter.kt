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

import cn.enaium.jimmer.buddy.extensions.template.BuddyTemplateFile
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
        "version" to "0.9.106"
    )

    private val versionKotlin = mapOf(
        "name" to "kotlin",
        "version" to "2.2.10"
    )

    private val versionKsp = mapOf(
        "name" to "ksp",
        "version" to "2.2.10+"
    )

    private val versionSpringDependency = mapOf(
        "name" to "springDependency",
        "version" to "1.1.7"
    )

    private val versionSpringBoot = mapOf(
        "name" to "springboot",
        "version" to "3.5.5"
    )

    private val libJimmerApt = mapOf(
        "configuration" to "annotationProcessor",
        "name" to "jimmer-apt",
        "group" to "org.babyfish.jimmer",
        "versionRef" to "jimmer",
        "alias" to "jimmer.apt"
    )

    private val libJimmerKsp = mapOf(
        "configuration" to "ksp",
        "name" to "jimmer-ksp",
        "group" to "org.babyfish.jimmer",
        "versionRef" to "jimmer",
        "alias" to "jimmer.ksp"
    )

    private val libJimmerCore = mapOf(
        "configuration" to "implementation",
        "name" to "jimmer-core",
        "group" to "org.babyfish.jimmer",
        "versionRef" to "jimmer",
        "alias" to "jimmer.core"
    )

    private val libJimmerSql = mapOf(
        "configuration" to "implementation",
        "name" to "jimmer-sql",
        "group" to "org.babyfish.jimmer",
        "versionRef" to "jimmer",
        "alias" to "jimmer.sql"
    )

    private val libJimmerSqlKotlin = mapOf(
        "configuration" to "implementation",
        "name" to "jimmer-sql-kotlin",
        "group" to "org.babyfish.jimmer",
        "versionRef" to "jimmer",
        "alias" to "jimmer.sql.kotlin"
    )

    private val libJimmerSpringBootStarter = mapOf(
        "configuration" to "implementation",
        "name" to "jimmer-spring-boot-starter",
        "group" to "org.babyfish.jimmer",
        "versionRef" to "jimmer",
        "alias" to "jimmer.spring.boot.starter"
    )

    private val libSpringBootStaterWeb = mapOf(
        "configuration" to "implementation",
        "name" to "spring-boot-starter-web",
        "group" to "org.springframework.boot",
        "alias" to "spring.boot.starter.web"
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

    private val pluginSpringDependency = mapOf(
        "name" to "spring-dependency-management",
        "id" to "io.spring.dependency-management",
        "versionRef" to "springDependency",
        "alias" to "spring.dependency.management"
    )

    private val pluginSpringBoot = mapOf(
        "name" to "spring-boot",
        "id" to "org.springframework.boot",
        "versionRef" to "springboot",
        "alias" to "spring.boot"
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
        } + when (projectModel.type) {
            JimmerProjectModel.Type.SPRING_BOOT -> {
                listOf(versionSpringDependency, versionSpringBoot)
            }

            JimmerProjectModel.Type.SQL -> {
                listOf()
            }

            JimmerProjectModel.Type.IMMUTABLE -> {
                listOf()
            }
        }

        val libs = when (projectModel.type) {
            JimmerProjectModel.Type.SPRING_BOOT -> {
                listOf(libJimmerSpringBootStarter, libSpringBootStaterWeb)
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
        } + when (projectModel.builder) {
            JimmerProjectModel.Builder.GRADLE -> when (projectModel.language) {
                JimmerProjectModel.Language.KOTLIN -> {
                    listOf(libJimmerKsp)
                }

                JimmerProjectModel.Language.JAVA -> {
                    listOf(libJimmerApt)
                }
            }

            JimmerProjectModel.Builder.MAVEN -> listOf()
        }

        val plugins = when (projectModel.language) {
            JimmerProjectModel.Language.KOTLIN -> {
                listOf(pluginKotlinJvm, pluginKsp)
            }

            JimmerProjectModel.Language.JAVA -> {
                listOf()
            }
        } + when (projectModel.type) {
            JimmerProjectModel.Type.SPRING_BOOT -> {
                listOf(pluginSpringDependency, pluginSpringBoot)
            }

            JimmerProjectModel.Type.SQL -> {
                listOf()
            }

            JimmerProjectModel.Type.IMMUTABLE -> {
                listOf()
            }
        }

        val fileTemplateManager = FileTemplateManager.getInstance(project)
        when (projectModel.builder) {
            JimmerProjectModel.Builder.GRADLE -> {
                val gradleWrapper = fileTemplateManager.getInternalTemplate(BuddyTemplateFile.GRADLE_WRAPPER)
                val gradleWrapperContent =
                    gradleWrapper.getText(mapOf("WRAPPER_VERSION" to projectModel.wrapperVersion))
                val gradleWrapperPath = projectDir.resolve("gradle/wrapper/gradle-wrapper.properties")
                if (gradleWrapperPath.exists().not()) {
                    gradleWrapperPath.createParentDirectories()
                }
                gradleWrapperPath.writeText(gradleWrapperContent)
                val gradleToml = fileTemplateManager.getInternalTemplate(BuddyTemplateFile.GRADLE_TOML)
                val gradleTomlContent = gradleToml.getText(
                    mapOf(
                        "versions" to versions,
                        "libraries" to libs,
                        "plugins" to plugins
                    )
                )
                val gradleTomlPath = projectDir.resolve("gradle/libs.versions.toml")
                gradleTomlPath.writeText(gradleTomlContent)
                val gradleBuild = fileTemplateManager.getInternalTemplate(BuddyTemplateFile.GRADLE_BUILD)
                val gradleBuildContent = gradleBuild.getText(
                    mapOf(
                        "GROUP" to projectModel.group,
                        "plugins" to plugins,
                        "libraries" to libs
                    )
                )
                val gradleBuildPath = projectDir.resolve("build.gradle.kts")
                gradleBuildPath.writeText(gradleBuildContent)
                val gradleSettings = fileTemplateManager.getInternalTemplate(BuddyTemplateFile.GRADLE_SETTINGS)
                val gradleSettingsContent = gradleSettings.getText(mapOf("ARTIFACT" to projectModel.artifact))
                val gradleSettingsPath = projectDir.resolve("settings.gradle.kts")
                gradleSettingsPath.writeText(gradleSettingsContent)
            }

            JimmerProjectModel.Builder.MAVEN -> {
                val mavenWrapper = fileTemplateManager.getInternalTemplate(BuddyTemplateFile.MAVEN_WRAPPER)
                val mavenWrapperContent = mavenWrapper.getText(mapOf("WRAPPER_VERSION" to projectModel.wrapperVersion))
                val mavenWrapperPath = projectDir.resolve(".mvn/wrapper/maven-wrapper.properties")
                if (mavenWrapperPath.exists().not()) {
                    mavenWrapperPath.createParentDirectories()
                }
                mavenWrapperPath.writeText(mavenWrapperContent)
                val mavenPom = fileTemplateManager.getInternalTemplate(BuddyTemplateFile.MAVEN_POM)
                val mavenPomContent = mavenPom.getText(
                    mapOf(
                        "GROUP" to projectModel.group,
                        "ARTIFACT" to projectModel.artifact,
                        "versions" to versions.filter { it != versionSpringDependency },
                        "libraries" to libs,
                        "apt" to libJimmerApt,
                        "dependencyManagements" to listOf(
                            mapOf(
                                "name" to "spring-boot-dependencies",
                                "id" to "org.springframework.boot",
                                "versionRef" to "springboot",
                            )
                        ),
                        "plugins" to listOf(
                            mapOf(
                                "name" to "spring-boot-maven-plugin",
                                "id" to "org.springframework.boot",
                                "versionRef" to "springboot"
                            )
                        )
                    )
                )
                val mavenPomPath = projectDir.resolve("pom.xml")
                mavenPomPath.writeText(mavenPomContent)
            }
        }
    }
}