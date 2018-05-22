import org.gradle.api.publish.maven.*

import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig
import org.jfrog.gradle.plugin.artifactory.dsl.ResolverConfig

import groovy.lang.GroovyObject

group = "org.gradle.gradle2kts"

version = "0.0.1-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.2.41" 
    `maven-publish`
    id("com.jfrog.artifactory") version "4.1.1"
}

repositories {
    jcenter()
}

dependencies {

    compile(gradleApi())
    compile(kotlin("stdlib"))
    compile(kotlin("reflect"))

    testCompile("junit:junit:4.12")
}

publishing {
    (publications) {
        "mavenJava"(MavenPublication::class) {
            from(components["java"])
        }
    }
}

artifactory {
    setContextUrl("https://repo.gradle.org/gradle")
    publish(delegateClosureOf<PublisherConfig> {
        repository(delegateClosureOf<GroovyObject> {
            val targetRepoKey = "libs-${buildTagFor(project.version as String)}s-local"
            setProperty("repoKey", targetRepoKey)
            setProperty("username", project.findProperty("artifactory_user") ?: "nouser")
            setProperty("password", project.findProperty("artifactory_password") ?: "nopass")
            setProperty("maven", true)
        })
        defaults(delegateClosureOf<GroovyObject> {
            invokeMethod("publications", "mavenJava")
        })
    })
    resolve(delegateClosureOf<ResolverConfig> {
        setProperty("repoKey", "repo")
    })
}

fun buildTagFor(version: String): String =
    when (version.substringAfterLast('-')) {
        "SNAPSHOT" -> "snapshot"
        in Regex("""M\d+[a-z]*$""") -> "milestone"
        else -> "release"
    }

operator fun Regex.contains(s: String) = matches(s)
