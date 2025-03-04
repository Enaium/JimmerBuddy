import org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask

plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij)
}
group = "cn.enaium"
version = "${property("version")}-242"

sourceSets {
    main {
        kotlin.srcDir("../shared/main/kotlin")
        java.srcDir("../shared/main/java")
        resources.srcDir("../shared/main/resources")
    }
}

repositories {
    mavenCentral()
    intellijPlatform {
        releases()
        marketplace()
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.2.1")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
    }

    implementation(libs.jimmer.core)
    implementation(libs.jimmer.apt)
    implementation(libs.jimmer.ksp)
    implementation(libs.jimmer.dto.compiler)
    implementation(libs.symbol.processing.api)
    implementation(libs.byte.buddy)
    implementation(libs.kotlinpoet)
    implementation(libs.javapoet)
}

tasks {
    patchPluginXml {
        sinceBuild.set("242")
        untilBuild.set("262.*")
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

tasks.processResources {
    inputs.property("currentTimeMillis", System.currentTimeMillis())
    filesMatching("plugin.xml") {
        expand(
            mapOf(
                "supportsK2" to true
            )
        )
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.named<RunIdeTask>("runIde") {
    jvmArgumentProviders += CommandLineArgumentProvider {
        listOf("-Didea.kotlin.plugin.use.k2=true")
    }
}