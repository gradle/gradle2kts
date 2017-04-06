# gradle2kts

Gradle Groovy to Gradle Kotlin conversion tool.

```Groovy
task compile {
    doLast {
        println 'compiling source'
    }
}

task compileTest(dependsOn: compile) {
    doLast {
        println 'compiling unit tests'
    }
}

task test(dependsOn: [compile, compileTest]) {
    doLast {
        println 'running unit tests'
    }
}

task dist(dependsOn: [compile, test]) {
    doLast {
        println 'building the distribution'
    }
}
```

Becomes:

```Kotlin
val compile by tasks.creating {
    doLast {
        println("compiling source")
    }
}

val compileTest by tasks.creating {
    dependsOn(compile)
    doLast {
        println("compiling unit tests")
    }
}

val test by tasks.creating {
    dependsOn(compile, compileTest)
    doLast {
        println("running unit tests")
    }
}

val dist by tasks.creating {
    dependsOn(compile, test)
    doLast {
        println("building the distribution")
    }
}
```
