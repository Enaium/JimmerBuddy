import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("intellij-platform")
    alias(libs.plugins.grammar)
}
group = "cn.enaium"
version = "${property("version")}"

dependencies {
    implementation(libs.jimmer.core)
    implementation(libs.jimmer.apt)
    implementation(libs.jimmer.ksp)
    implementation(libs.jimmer.sql)
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
    implementation(libs.jspecify)
    implementation(project(":gradle-tooling-extension"))
}

sourceSets {
    main {
        java.srcDir("src/main/gen")
    }
}

tasks {
    generateLexer {
        sourceFile.set(file("src/main/grammars/DtoLexer.flex"))
        targetOutputDir.set(file("src/main/gen/cn/enaium/jimmer/buddy/extensions/dto/lexer"))
        purgeOldFiles.set(true)
    }

    generateParser {
        sourceFile.set(file("src/main/grammars/DtoParser.bnf"))
        targetRootOutputDir.set(file("src/main/gen"))
        pathToParser.set("cn/enaium/jimmer/buddy/extensions/dto/parser/DtoParser.java")
        pathToPsiRoot.set("cn/enaium/jimmer/buddy/extensions/dto/psi")
        purgeOldFiles.set(true)
    }

    withType<KotlinCompile> {
        dependsOn(generateLexer, generateParser)
    }
}