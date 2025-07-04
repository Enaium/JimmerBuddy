plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.intellij) apply false
}

subprojects {
    configurations.all {
        exclude(module = "icu4j")
        exclude(module = "spring-core")
        exclude(module = "commons-lang3")
        exclude(module = "jackson-core")
        exclude(module = "jackson-databind")
        exclude(module = "jackson-datatype-jsr310")
    }
}