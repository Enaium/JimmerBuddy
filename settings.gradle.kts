rootProject.name = "JimmerBuddy"
when (gradle.startParameter.projectProperties["sinceBuild"]) {
    "242" -> include("242")
    else -> include("231")
}
