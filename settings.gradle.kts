rootProject.name = "JimmerBuddy"
when (gradle.startParameter.projectProperties["sinceBuild"]) {
    "232" -> include("232")
    else -> include("242")
}
