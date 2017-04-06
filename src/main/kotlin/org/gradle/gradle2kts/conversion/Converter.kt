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

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.GroovyCodeVisitor
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ArrayExpression
import org.codehaus.groovy.ast.expr.AttributeExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.BitwiseNegationExpression
import org.codehaus.groovy.ast.expr.BooleanExpression
import org.codehaus.groovy.ast.expr.CastExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ClosureListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.ElvisOperatorExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.FieldExpression
import org.codehaus.groovy.ast.expr.GStringExpression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.MethodPointerExpression
import org.codehaus.groovy.ast.expr.NotExpression
import org.codehaus.groovy.ast.expr.PostfixExpression
import org.codehaus.groovy.ast.expr.PrefixExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.RangeExpression
import org.codehaus.groovy.ast.expr.SpreadExpression
import org.codehaus.groovy.ast.expr.SpreadMapExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.expr.TernaryExpression
import org.codehaus.groovy.ast.expr.TupleExpression
import org.codehaus.groovy.ast.expr.UnaryMinusExpression
import org.codehaus.groovy.ast.expr.UnaryPlusExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.AssertStatement
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.BreakStatement
import org.codehaus.groovy.ast.stmt.CaseStatement
import org.codehaus.groovy.ast.stmt.CatchStatement
import org.codehaus.groovy.ast.stmt.ContinueStatement
import org.codehaus.groovy.ast.stmt.DoWhileStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ForStatement
import org.codehaus.groovy.ast.stmt.IfStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.stmt.SwitchStatement
import org.codehaus.groovy.ast.stmt.SynchronizedStatement
import org.codehaus.groovy.ast.stmt.ThrowStatement
import org.codehaus.groovy.ast.stmt.TryCatchStatement
import org.codehaus.groovy.ast.stmt.WhileStatement
import org.codehaus.groovy.classgen.BytecodeExpression
import org.gradle.gradle2kts.ast.KConstant
import org.gradle.gradle2kts.ast.KIdentifier
import org.gradle.gradle2kts.ast.KInfix
import org.gradle.gradle2kts.ast.KInvocation
import org.gradle.gradle2kts.ast.KLambda
import org.gradle.gradle2kts.ast.KMember
import org.gradle.gradle2kts.ast.KNode
import org.gradle.gradle2kts.ast.KStringTemplate
import org.gradle.gradle2kts.ast.pack


/**
 * Converts a Groovy AST to a Kotlin AST.
 */
fun convert(code: Statement): KNode =
    Converter().convert(code)


private
class Converter : GroovyCodeVisitor {

    fun convert(ast: ASTNode): KNode {
        accept(ast)
        return pop()
    }

    fun accept(ast: ASTNode) {
        ast.visit(this)
    }

    var result: KNode? = null

    fun pop(): KNode {
        val node = result!!
        result = null
        return node
    }

    fun push(node: KNode) {
        require(result == null)
        result = node
    }

    override fun visitBlockStatement(block: BlockStatement) {
        val nodes = convertAll(block.statements)
        push(pack(nodes))
    }

    fun convertAll(xs: Iterable<ASTNode>) =
        xs.map { convert(it) }

    override fun visitExpressionStatement(statement: ExpressionStatement) {
        accept(statement.expression)
    }

    override fun visitMethodCallExpression(call: MethodCallExpression) {
        push(
            KInvocation(
                invocationReceiverFrom(call.method),
                invocationArgumentsFrom(call.arguments)))
    }

    fun invocationReceiverFrom(m: Expression): KNode =
        when (m) {
            is ConstantExpression -> KIdentifier(m.text)
            else                  -> convert(m)
        }

    fun invocationArgumentsFrom(a: Expression): List<KNode> =
        when (a) {
            is TupleExpression -> convertAll(a.expressions)
            else               -> listOf(convert(a))
        }

    override fun visitClosureExpression(expression: ClosureExpression) {
        push(KLambda(convert(expression.code)))
    }

    override fun visitListExpression(expression: ListExpression) {
        push(KInvocation(listOf, convertAll(expression.expressions)))
    }

    override fun visitMapExpression(expression: MapExpression) {
        push(KInvocation(mapOf, convertAll(expression.mapEntryExpressions)))
    }

    override fun visitMapEntryExpression(expression: MapEntryExpression) {
        expression.run {
            push(KInfix(to, convert(keyExpression), convert(valueExpression)))
        }
    }

    companion object Identifiers {
        val listOf = KIdentifier("listOf")
        val mapOf = KIdentifier("mapOf")
        val to = KIdentifier("to")
    }

    override fun visitVariableExpression(expression: VariableExpression) {
        push(KIdentifier(expression.name))
    }

    override fun visitPropertyExpression(expression: PropertyExpression) {
        push(KMember(convert(expression.objectExpression), KIdentifier(expression.propertyAsString)))
    }

    override fun visitBinaryExpression(expression: BinaryExpression) {
        expression.run {
            push(KInfix(KIdentifier(operation.text), convert(leftExpression), convert(rightExpression)))
        }
    }

    override fun visitConstantExpression(expression: ConstantExpression) {
        push(KConstant(expression.value))
    }

    override fun visitGStringExpression(expression: GStringExpression) {
        push(KStringTemplate(stringTemplatePartsFrom(expression)))
    }

    fun stringTemplatePartsFrom(expression: GStringExpression): List<KNode> =
        expression.run {
            strings.foldIndexed(mutableListOf<KNode>()) { index, parts, string ->
                parts.add(convert(string))
                values.elementAtOrNull(index)?.let { parts.add(convert(it)) }
                parts
            }
        }

    override fun visitContinueStatement(statement: ContinueStatement?) {
        TODO("visitContinueStatement")
    }

    override fun visitArrayExpression(expression: ArrayExpression?) {
        TODO("visitArrayExpression")
    }

    override fun visitIfElse(ifElse: IfStatement?) {
        TODO("visitIfElse")
    }

    override fun visitPrefixExpression(expression: PrefixExpression?) {
        TODO("visitPrefixExpression")
    }

    override fun visitDeclarationExpression(expression: DeclarationExpression?) {
        TODO("visitDeclarationExpression")
    }

    override fun visitBitwiseNegationExpression(expression: BitwiseNegationExpression?) {
        TODO("visitBitwiseNegationExpression")
    }

    override fun visitForLoop(forLoop: ForStatement?) {
        TODO("visitForLoop")
    }

    override fun visitMethodPointerExpression(expression: MethodPointerExpression?) {
        TODO("visitMethodPointerExpression")
    }

    override fun visitArgumentlistExpression(expression: ArgumentListExpression?) {
        TODO("visitArgumentlistExpression")
    }

    override fun visitClosureListExpression(closureListExpression: ClosureListExpression?) {
        TODO("visitClosureListExpression")
    }

    override fun visitThrowStatement(statement: ThrowStatement?) {
        TODO("visitThrowStatement")
    }

    override fun visitAssertStatement(statement: AssertStatement?) {
        TODO("visitAssertStatement")
    }

    override fun visitCastExpression(expression: CastExpression?) {
        TODO("visitCastExpression")
    }

    override fun visitPostfixExpression(expression: PostfixExpression?) {
        TODO("visitPostfixExpression")
    }

    override fun visitBytecodeExpression(expression: BytecodeExpression?) {
        TODO("visitBytecodeExpression")
    }

    override fun visitBreakStatement(statement: BreakStatement?) {
        TODO("visitBreakStatement")
    }

    override fun visitClassExpression(expression: ClassExpression?) {
        TODO("visitClassExpression")
    }

    override fun visitWhileLoop(loop: WhileStatement?) {
        TODO("visitWhileLoop")
    }

    override fun visitBooleanExpression(expression: BooleanExpression?) {
        TODO("visitBooleanExpression")
    }

    override fun visitRangeExpression(expression: RangeExpression?) {
        TODO("visitRangeExpression")
    }

    override fun visitShortTernaryExpression(expression: ElvisOperatorExpression?) {
        TODO("visitShortTernaryExpression")
    }

    override fun visitCaseStatement(statement: CaseStatement?) {
        TODO("visitCaseStatement")
    }

    override fun visitTupleExpression(expression: TupleExpression?) {
        TODO("visitTupleExpression")
    }

    override fun visitDoWhileLoop(loop: DoWhileStatement?) {
        TODO("visitDoWhileLoop")
    }

    override fun visitFieldExpression(expression: FieldExpression?) {
        TODO("visitFieldExpression")
    }

    override fun visitUnaryMinusExpression(expression: UnaryMinusExpression?) {
        TODO("visitUnaryMinusExpression")
    }

    override fun visitTernaryExpression(expression: TernaryExpression?) {
        TODO("visitTernaryExpression")
    }

    override fun visitTryCatchFinally(finally1: TryCatchStatement?) {
        TODO("visitTryCatchFinally")
    }

    override fun visitReturnStatement(statement: ReturnStatement?) {
        TODO("visitReturnStatement")
    }

    override fun visitStaticMethodCallExpression(expression: StaticMethodCallExpression?) {
        TODO("visitStaticMethodCallExpression")
    }

    override fun visitConstructorCallExpression(expression: ConstructorCallExpression?) {
        TODO("visitConstructorCallExpression")
    }

    override fun visitSpreadMapExpression(expression: SpreadMapExpression?) {
        TODO("visitSpreadMapExpression")
    }

    override fun visitNotExpression(expression: NotExpression?) {
        TODO("visitNotExpression")
    }

    override fun visitUnaryPlusExpression(expression: UnaryPlusExpression?) {
        TODO("visitUnaryPlusExpression")
    }

    override fun visitCatchStatement(statement: CatchStatement?) {
        TODO("visitCatchStatement")
    }

    override fun visitSynchronizedStatement(statement: SynchronizedStatement?) {
        TODO("visitSynchronizedStatement")
    }

    override fun visitSwitch(statement: SwitchStatement?) {
        TODO("visitSwitch")
    }

    override fun visitSpreadExpression(expression: SpreadExpression?) {
        TODO("visitSpreadExpression")
    }

    override fun visitAttributeExpression(attributeExpression: AttributeExpression?) {
        TODO("visitAttributeExpression")
    }
}
