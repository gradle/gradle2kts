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

/**
 * Functions missing from the Kotlin standard library.
 */
package org.gradle.gradle2kts


/**
 * Left-to-right function composition.
 */
inline
infix fun <S, T, U> ((S) -> T).then(crossinline g: (T) -> U): (S) -> U =
    let { f -> { g(f(it)) } }


inline
fun <reified T> Any?.isA(): T? =
    this as? T