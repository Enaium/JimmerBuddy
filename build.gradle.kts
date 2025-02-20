plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij)
}
group = "cn.enaium"
version = "0.1.0"

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
    implementation(libs.jimmer.dto.compiler)
}

tasks {
    patchPluginXml {
        sinceBuild.set("232")
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

kotlin {
    jvmToolchain(17)
}