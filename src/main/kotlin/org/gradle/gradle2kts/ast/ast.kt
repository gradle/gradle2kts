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

sealed class KNode

/**
 * `<value>`
 */
data class KConstant(val value: Any) : KNode()

/**
 * `"$part0...$partN"`
 */
data class KStringTemplate(val parts: List<KNode>) : KNode()

/**
 * `<name>`
 */
data class KIdentifier(val name: String) : KNode()

/**
 * `<parent>.<name>`
 */
data class KMember(val parent: KNode, val name: KIdentifier) : KNode()

/**
 * `<receiver>(<args>)`
 */
data class KInvocation(val receiver: KNode, val args: List<KNode>) : KNode()

/**
 * `<name> = <value>`
 */
data class KNamed(val name: KIdentifier, val value: KNode) : KNode()

/**
 * `<left> <operator> <right>`
 */
data class KInfix(val operator: KIdentifier, val left: KNode, val right: KNode) : KNode()

/**
 * `{ <body> }`
 */
data class KLambda(val body: KNode) : KNode()

/**
 * `child0; child1; ... childN;`
 */
data class KBlock(val children: List<KNode>) : KNode()

/**
 * `val <name> by <delegate>`
 */
data class KDelegate(val name: KIdentifier, val delegate: KNode) : KNode()

/**
 * `this`
 */
object KThis : KNode()


fun pack(nodes: List<KNode>) = if (nodes.size == 1) nodes.first() else KBlock(nodes)


val kConstant = ofType<KConstant>()

val kConstantValue = kConstant / KConstant::value

val kIdentifier = ofType<KIdentifier>()

val kIdentifierName = kIdentifier / KIdentifier::name

val kLambda = ofType<KLambda>()

val kString = kConstantValue / ofType<String>()

val kInfix = ofType<KInfix>()

val kOperatorName = KInfix::operator / KIdentifier::name

val kTuple = kInfix containing (kOperatorName / ofValue("to"))

val kInvocation = ofType<KInvocation>()

fun kInvocationOf(functionName: String) =
    kInvocation containing (KInvocation::receiver / kIdentifierName / ofValue(functionName))

fun <T : KNode> kNodeAsList(node: T) =
    listOf(node)

fun kElements(fromIndex: Int): Prism<List<KNode>, List<KNode>> =
    { it.drop(fromIndex) }

fun kElement(at: Int): Prism<List<KNode>, KNode> =
    { it.elementAtOrNull(at) }

val kLast: Prism<List<KNode>, KNode> = { it.lastOrNull() }

val kAllButLast: Prism<List<KNode>, List<KNode>> = { it.dropLast(1) }


fun bottomUpTransform(f: Prism<KNode, KNode>): (KNode) -> KNode {

    fun mapIdentifier(x: KIdentifier): KIdentifier = f(x)?.let { it as KIdentifier } ?: x

    fun transform(node: KNode): KNode = node.run {
        when (this) {
            is KBlock          -> KBlock(children.map(::transform))
            is KDelegate       -> KDelegate(mapIdentifier(name), transform(delegate))
            is KMember         -> KMember(transform(parent), mapIdentifier(name))
            is KInvocation     -> KInvocation(transform(receiver), args.map(::transform))
            is KLambda         -> KLambda(transform(body))
            is KNamed          -> KNamed(mapIdentifier(name), transform(value))
            is KInfix          -> KInfix(mapIdentifier(operator), transform(left), transform(right))
            is KStringTemplate -> KStringTemplate(parts.map(::transform))
            is KIdentifier     -> this
            is KConstant       -> this
            KThis              -> this
        }.let { f(it) ?: it }
    }
    return ::transform
}
