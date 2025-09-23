plugins {
    java
    id("intellij-platform")
}
group = "cn.enaium"
version = "${property("version")}"

dependencies {
    implementation(libs.jimmer.core)
    implementation(libs.jimmer.apt)
    implementation(libs.jimmer.ksp)
    implementation(libs.jimmer.sql)
    implementation(libs.jimmer.client)
    implementation(libs.jimmer.dto.compiler)
    implementation(libs.jimmer.spring.boot.starter) {
        exclude(module = "*")
    }
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