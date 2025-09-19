subprojects {
    configurations.all {
        exclude(module = "icu4j")
        exclude(module = "spring-core")
        exclude(module = "commons-lang3")
    }
}