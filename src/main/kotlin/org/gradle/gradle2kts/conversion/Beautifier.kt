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

import org.gradle.gradle2kts.ast.*

val kMapOfArgs = kInvocationOf("mapOf") / KInvocation::args

val kListOfArgs = kInvocationOf("listOf") / KInvocation::args

val kTaskInvocationArgs = kInvocationOf("task") / KInvocation::args

val kComplexTaskArgs =
    (kElement(0) / kMapOfArgs) * (kElement(1) / kString) * kElements(fromIndex = 2)

val kLastLambda = kLast / kLambda

val taskDelegateFromComplexTask =
    kComplexTaskArgs / { (taskProperties, taskName, remaining) ->
        (kLastLambda * kAllButLast)(remaining)
            ?.let { (lambda, allButLast) ->
                allButLast + taskLambdaFor(taskProperties, originalBody = lambda.body)
            }.let {
                it ?: remaining + taskLambdaFor(taskProperties)
            }.let { taskDelegateArgs ->
                taskDelegate(taskName, taskDelegateArgs)
            }
    }


val taskDelegateFromSimpleTask =
    (kElement(0) / kString) * kElements(fromIndex = 1) / { (taskName, taskArgs) ->
        taskDelegate(taskName, taskArgs)
    }


val beautify =
    bottomUpTransform(
        kTaskInvocationArgs /
            (taskDelegateFromComplexTask or taskDelegateFromSimpleTask))


val kTaskProperty =
    kTuple / ((KInfix::left / kString) * (KInfix::right / (kListOfArgs or ::kNodeAsList)))

val taskPropertyInvocation =
    kTaskProperty / { (taskName, args) ->
        KInvocation(KIdentifier(taskName), args)
    }

fun taskLambdaFor(taskProperties: List<KNode>, originalBody: KNode? = null) =
    taskProperties
        .map { taskPropertyInvocation(it) ?: it }
        .let { propertyInvocations -> originalBody?.let { propertyInvocations + it } ?: propertyInvocations }
        .let { body -> KLambda(pack(body)) }

fun taskDelegate(taskName: String, args: List<KNode>): KDelegate =
    KDelegate(KIdentifier(taskName), if (args.isEmpty()) tasksCreating else KInvocation(tasksCreating, args))

val tasksCreating = KMember(KIdentifier("tasks"), KIdentifier("creating"))
