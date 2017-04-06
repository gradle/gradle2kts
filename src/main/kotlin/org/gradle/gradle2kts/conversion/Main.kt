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

import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.Phases
import org.gradle.api.internal.project.ProjectScript
import org.gradle.gradle2kts.then
import org.gradle.groovy.scripts.internal.TaskDefinitionScriptTransformer


/**
 * Converts Gradle Groovy script code to Gradle Kotlin script code.
 */
val gradle2kts = ::canonicalizeGradleCode then ::convert then beautify then ::prettyPrint


private
fun canonicalizeGradleCode(code: String): Statement =
    CompilationUnit().run {
        configuration.scriptBaseClass = ProjectScript::class.qualifiedName
        addSource("build.gradle", code)
        canonicalize()
        firstClassNode
            .getMethod("run", emptyArray())
            .code
    }


private
fun CompilationUnit.canonicalize() {
    TaskDefinitionScriptTransformer().register(this)
    compile(Phases.CANONICALIZATION)
}

