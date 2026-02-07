subprojects {
    configurations.all {
        exclude(module = "icu4j")
        exclude(module = "spring-core")
        exclude(module = "commons-lang3")
    }
}

tasks.register("buildPlugins") {
    dependsOn(listOf("231", "242", "253").map { project(":since:$it").tasks.named("buildPlugin") })
}