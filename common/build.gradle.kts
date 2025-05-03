plugins {
    antlr
    id("java")
}

group = "cn.enaium"
version = "${property("version")}"

repositories {
    mavenCentral()
}

dependencies {
    antlr(libs.antlr)
}

tasks.test {
    useJUnitPlatform()
}


tasks.withType<Jar>().configureEach {
    dependsOn(tasks.withType<AntlrTask>())
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn(tasks.withType<AntlrTask>())
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}