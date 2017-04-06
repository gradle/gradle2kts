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
import org.gradle.gradle2kts.isA


fun prettyPrint(root: KNode): String =
    PrettyPrinter().prettyPrint(root)


private
class PrettyPrinter {

    val result = StringBuilder()
    var margin = 0

    fun prettyPrint(node: KNode): String {
        pp(node)
        return result.toString()
    }

    fun pp(node: KNode): Unit = node.run {
        when (this) {
            is KMember         -> {
                pp(parent)
                +"."
                pp(name)
            }
            is KInvocation     -> {
                pp(receiver)
                when {
                    args.lastOrNull() is KLambda -> {
                        if (args.size > 1) {
                            parenthesize { ppList(args.dropLast(1)) }
                        }
                        +" "
                        pp(args.last())
                    }
                    else                         -> {
                        parenthesize { ppList(args) }
                    }
                }
            }
            is KDelegate       -> {
                +"val "
                pp(name)
                +" by "
                pp(delegate)
            }
            is KInfix          -> {
                pp(left)
                +" "
                pp(operator)
                +" "
                pp(right)
            }
            is KIdentifier     -> {
                +name
            }
            is KConstant       -> {
                when (value) {
                    is String -> +"\"$value\""
                    else      -> +value.toString()
                }
            }
            is KLambda         -> {
                +"{"
                indented(body)
                +"}"
            }
            is KBlock          -> {
                ppList(children) {
                    newLine()
                }
            }
            is KStringTemplate -> {
                +"\""
                parts.forEach { kStringTemplatePart(it) }
                +"\""
            }
            else               -> {
                throw IllegalArgumentException("Unsupported node: $node")
            }
        }
    }

    val kStringTemplatePart =
        ((kString / { +it })
            or (kIdentifierName / { +"${'$'}$it" })
            or { +"${'$'}{"; pp(it as KNode); +"}" })

    inline
    fun parenthesize(block: () -> Unit) {
        +"("
        block()
        +")"
    }

    fun ppList(nodes: List<KNode>, sep: () -> Unit = { +", " }) {
        if (nodes.isEmpty()) return
        pp(nodes.first())
        (1..nodes.lastIndex).forEach {
            sep()
            pp(nodes[it])
        }
    }

    fun indented(node: KNode) {
        if (isEmptyBlock(node)) {
            newLine()
        } else {
            indent()
            newLine()
            pp(node)
            dedent()
            newLine()
        }
    }

    fun isEmptyBlock(node: KNode) =
        node.isA<KBlock>()?.children?.isEmpty() ?: false

    fun indent() {
        margin += 4
    }

    fun dedent() {
        margin -= 4
    }

    fun newLine() {
        lineBreak()
        +" ".repeat(margin)
    }

    private fun lineBreak() {
        result.appendln()
    }

    operator fun String.unaryPlus() {
        result.append(this)
    }
}