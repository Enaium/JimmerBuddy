plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api(libs.kotlin)
    api(libs.intellij.platform)
    api(libs.changelog)
}