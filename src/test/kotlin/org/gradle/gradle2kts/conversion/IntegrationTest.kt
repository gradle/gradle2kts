/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.gradle2kts.conversion

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat

import org.junit.Test

class IntegrationTest {

    @Test fun `can convert simple task`() {
        assertConversion(
            from = "task foo",
            to = """val foo by tasks.creating""")
    }

    @Test fun `can convert task with doLast action`() {
        assertConversion(
            from = """
                task compile {
                    doLast {
                        println 'compiling source'
                    }
                }
            """,
            to = """
                val compile by tasks.creating {
                    doLast {
                        println("compiling source")
                    }
                }
            """)
    }

    @Test fun `can convert task with dependsOn`() {
        assertConversion(
            from = """
                task compileTest(dependsOn: compile) {
                    doLast {
                    }
                }
            """,
            to = """
                val compileTest by tasks.creating {
                    dependsOn(compile)
                    doLast {
                    }
                }
            """)
    }

    @Test fun `can convert task with dependsOn list`() {
        assertConversion(
            from = """
                task test(dependsOn: [compile, compileTest])
            """,
            to = """
                val test by tasks.creating {
                    dependsOn(compile, compileTest)
                }
            """)
    }

    @Test fun `can convert multiple tasks`() {
        assertConversion(
            from = """
                task compile
                task test(dependsOn: compile)
            """,
            to = """
                val compile by tasks.creating
                val test by tasks.creating {
                    dependsOn(compile)
                }
            """)
    }

    @Test fun `can convert property references`() {
        assertConversion(
            from = "println buildFile.name",
            to = "println(buildFile.name)")
    }

    @Test fun `can convert GString expressions`() {
        assertConversion(
            from = "println \"${'$'}greeting, ${'$'}thing!\"",
            to = "println(\"${'$'}greeting, ${'$'}thing!\")")
    }

    @Test fun `can convert assignment`() {
        assertConversion(
            from = "group = 'org.gradle'",
            to = """group = "org.gradle"""")
    }

    fun assertConversion(from: String, to: String) {
        assertThat(
            gradle2kts(from),
            equalTo(to.replaceIndent()))
    }
}