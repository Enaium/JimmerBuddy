plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij)
}
group = "cn.enaium"
version = "${property("version")}-232"

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
        intellijIdeaCommunity("2023.2")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
    }

    implementation(libs.jimmer.core)
    implementation(libs.jimmer.apt)
    implementation(libs.jimmer.ksp)
    implementation(libs.jimmer.dto.compiler)
    implementation(libs.symbol.processing.api)
    implementation(libs.byte.buddy)
}

tasks {
    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set("242.*")
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
                "supportsK2" to false
            )
        )
    }
}

kotlin {
    jvmToolchain(17)
}