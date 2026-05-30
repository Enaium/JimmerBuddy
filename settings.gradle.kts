plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "JimmerBuddy"

include("common")
include("core")
include("gradle-tooling-extension")
include("since")
include("since:231")
include("since:242")
include("since:253")
include("since:262")