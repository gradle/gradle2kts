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

package org.gradle.gradle2kts.tasks

import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.gradle2kts.conversion.gradle2kts

import java.io.File

/**
 * Copies and converts all `*.gradle*` files from [sourceDir] into [destDir].
 */
open class Gradle2Kts : DefaultTask() {

    @get:InputDirectory
    var sourceDir: File? = null

    @get:OutputDirectory
    var destDir: File? = null

    @TaskAction
    fun convert() {
        copyAllFiles()
        convertGradleFiles()
    }

    private
    fun copyAllFiles() {
        project.copy { spec ->
            spec.from(sourceDir!!)
            spec.into(destDir!!)
        }
    }

    private
    fun convertGradleFiles() {
        destDir!!
            .walkTopDown()
            .filter(this::isGradleFile)
            .forEach(this::convert)
    }

    private
    fun isGradleFile(file: File) =
        file.run {
            isFile && extension.startsWith("gradle") && nameWithoutExtension != "settings"
        }

    private
    fun convert(file: File) {
        println("Converting `${project.relativeProjectPath(file.path)}'...")
        file.writeText(safeGradle2kts(file))
    }

    private
    fun safeGradle2kts(file: File) =
        file.readText().let { code ->
            try { gradle2kts(code) }
            catch (e: Throwable) {
                reportConversionFailure(file)
                "// ERROR: $e\n\n$code"
            }
        }

    private
    fun reportConversionFailure(file: File) {
        logger.warn("Failed to convert `${project.relativeProjectPath(file.path)}")
    }
}
