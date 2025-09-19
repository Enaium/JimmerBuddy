import org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask

plugins {
    id("intellij-platform")
    id("since")
}

tasks.named<RunIdeTask>("runIde") {
    jvmArgumentProviders += CommandLineArgumentProvider {
        listOf("-Didea.kotlin.plugin.use.k2=true")
    }
}