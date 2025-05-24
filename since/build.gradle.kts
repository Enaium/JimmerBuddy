import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformExtension

plugins {
    java
    alias(libs.plugins.changelog) apply false
    alias(libs.plugins.intellij) apply false
}

subprojects {
    apply {
        plugin("java")
        plugin("org.jetbrains.changelog")
        plugin("org.jetbrains.intellij.platform")
    }

    group = "cn.enaium"
    version = "${rootProject.property("version")}-${project.name}"


    sourceSets {
        main {
            resources.srcDir("../shared/resources")
        }
    }

    tasks.processResources {
        inputs.property("currentTimeMillis", System.currentTimeMillis())
        filesMatching("plugin.xml") {
            expand(
                mapOf(
                    "supportsK2" to project.property("supportsK2")
                )
            )
        }
    }

    configure<IntelliJPlatformExtension> {
        configure<IntelliJPlatformExtension.PluginConfiguration> {
            description = markdownToHTML(rootProject.file("README.md").readText())
        }
    }
}