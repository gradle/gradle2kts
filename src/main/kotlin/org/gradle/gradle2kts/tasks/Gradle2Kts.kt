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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import org.gradle.gradle2kts.conversion.gradle2kts

import java.io.File


/**
 * Copies and converts all `*.gradle*` files from [sourceDir] into [destDir].
 */
open class Gradle2Kts : DefaultTask() {

    @get:InputDirectory
    lateinit var sourceDir: File

    @get:OutputDirectory
    lateinit var destDir: File

    @TaskAction
    fun convert() {
        copyAllFiles()
        convertGradleFiles()
    }

    private
    fun copyAllFiles() {
        project.copy { spec ->
            spec.from(sourceDir)
            spec.into(destDir)
        }
    }

    private
    fun convertGradleFiles() {

        val results =
            destDir
                .walkTopDown()
                .filter(this::isGradleFile)
                .map(this::convert)
                .toList()

        println(
            "%d snippets out of %d were successfully converted.".format(
                results.count { it }, results.size
            )
        )
    }

    private
    fun isGradleFile(file: File) =
        file.run {
            isFile && extension.startsWith("gradle") && nameWithoutExtension != "settings"
        }

    private
    fun convert(file: File): Boolean {
        println("Converting `${project.relativeProjectPath(file.path)}'...")

        var success = true

        val groovyCode = file.readText()
        val convertedCode =
            try {
                gradle2kts(groovyCode)
            } catch (e: Throwable) {
                success = false
                reportConversionFailure(file)
                "// ERROR: $e\n\n$groovyCode"
            }

        file.writeText(convertedCode)

        return success
    }

    private
    fun reportConversionFailure(file: File) {
        logger.warn("Failed to convert `${project.relativeProjectPath(file.path)}")
    }
}
