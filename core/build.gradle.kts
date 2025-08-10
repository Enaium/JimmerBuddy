plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij)
}
group = "cn.enaium"
version = "${property("version")}"

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
        intellijIdeaCommunity("2023.1")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
        bundledPlugin("com.intellij.gradle")
    }

    implementation(libs.jimmer.core)
    implementation(libs.jimmer.apt)
    implementation(libs.jimmer.ksp)
    implementation(libs.jimmer.sql)
    implementation(libs.jimmer.dto.compiler)
    api(libs.symbol.processing.api)
    implementation(libs.byte.buddy)
    implementation(libs.kotlinpoet)
    implementation(libs.javapoet)
    implementation(libs.antlr4.intellij.adaptor)
    implementation(libs.h2)
    implementation(libs.jackson)
    implementation(project(":common"))
    implementation(project(":gradle-tooling-extension"))
}

kotlin {
    jvmToolchain(17)
}
