import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformExtension

plugins {
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

group = "cn.enaium"
version = "${rootProject.property("version")}-${project.name}"

dependencies {
    implementation(project(":core"))
}

tasks {
    patchPluginXml {
        sinceBuild.set(project.property("sinceBuild").toString())
        untilBuild.set(project.property("untilBuild").toString())
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

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
                "supportsK1" to project.property("supportsK1"),
                "supportsK2" to project.property("supportsK2")
            )
        )
    }
}

configure<IntelliJPlatformExtension> {
    configure<IntelliJPlatformExtension.PluginConfiguration> {
        description = markdownToHTML(rootProject.file("description.md").readText())
        changeNotes = markdownToHTML(rootProject.file("changelog.md").readText())
    }
}