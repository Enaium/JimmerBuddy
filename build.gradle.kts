plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.intellij) apply false
}

subprojects {
    configurations.all {
        exclude(module = "icu4j")
        exclude(module = "spring-core")
        exclude(module = "commons-lang3")
    }
}