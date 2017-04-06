import org.gradle.api.publish.maven.*

buildscript {

    repositories {
        gradleScriptKotlin()
    }

    dependencies {
        classpath(kotlinModule("gradle-plugin"))
    }
}

group = "org.gradle.gradle2kts"

version = "0.0.1-SNAPSHOT"

plugins {
    `maven-publish`
}

apply {
    plugin("kotlin")
}

repositories {
    gradleScriptKotlin()
}

dependencies {

    compile(gradleApi())
    compile(kotlinModule("stdlib"))
    compile(kotlinModule("reflect"))

    testCompile("junit:junit:4.12")
}

publishing {
    (publications) {
        "mavenJava"(MavenPublication::class) {
            from(components["java"])
        }
    }
}

