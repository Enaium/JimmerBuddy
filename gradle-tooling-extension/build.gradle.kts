plugins {
    java
    id("org.jetbrains.kotlin.jvm")
}
group = "cn.enaium"
version = "${property("version")}"

repositories {
    mavenCentral()
    maven {
        url = uri("https://www.jetbrains.com/intellij-repository/releases")
    }
    maven {
        url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    }
}

dependencies {
    compileOnly(libs.gradle.tooling.extension)
}

kotlin {
    jvmToolchain(8)
}