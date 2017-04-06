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

package org.gradle.gradle2kts.ast

/**
 * A `Prism<S, T>` can potentially extract a `T` from a `S` with failure indicated by `null`.
 */
typealias Prism<S, T> = (S) -> T?

/**
 * Prism composition.
 *
 * Given a prism `f` from [S] to [T], and a prism [g] from [T] to [U],
 * `f / g` gives their composition, a prism from [S] to [U].
 *
 * A.K.A: left-to-right Kleisli composition of the Maybe monad.
 */
inline
operator fun <S, T, U> Prism<S, T>.div(crossinline g: Prism<T, U>): Prism<S, U> =
    let { f -> { f(it)?.let(g) } }

/**
 * Prism prioritized choice.
 *
 * Given two prisms `f` and `g` from [S] to [T], `f or g` gives another prism from [S] to [T].
 *
 * The resulting prism will evaluate `f` and `g` in sequence short-circuiting on the first successful value.
 */
inline
infix fun <S, T> Prism<S, T>.or(crossinline g: Prism<S, T>): Prism<S, T> =
    let { f -> { f(it) ?: g(it) } }

/**
 * Prism fan-out.
 *
 * Given a prism `f` from [S] to [T], and a prism [g] from [T] to [U],
 * `f * g` gives a prism from [S] to [Pair]<[T], [U]>`.
 *
 * The resulting prism evaluates its input against both prisms and pair their outputs when they both succeed.
 */
// BUG: this function can't be marked inline otherwise the Kotlin compiler will emit invalid code in non clean builds
operator fun <S, T, U> Prism<S, T>.times(g: Prism<S, U>): Prism<S, Pair<T, U>> =
    let { f -> { f(it)?.let { left -> g(it)?.let { right -> left to right } } } }

/**
 * Prism constraining.
 *
 * Given a prism `f` from [S] to [T], and a prism [g] from [T] to [U],
 * `f containing g` gives a constrained `f` from [S] to [T] that only succeeds
 * if the composition `f / g` also succeeds.
 */
inline
infix fun <S, T, U> Prism<S, T>.containing(crossinline g: Prism<T, U>): Prism<S, T> =
    let { f -> { f(it)?.takeIf { g(it) != null } } }

/**
 * Accepts values of type [T].
 */
inline
fun <reified T> ofType(): Prism<Any, T> =
    { it as? T }

/**
 * Accepts values equal to [v].
 */
fun <T> ofValue(v: T): Prism<T, T> =
    { it.takeIf { it == v } }
