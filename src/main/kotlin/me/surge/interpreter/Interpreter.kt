package me.surge.interpreter

import me.surge.Constants
import me.surge.lexer.error.Error
import me.surge.lexer.error.context.Context
import me.surge.lexer.error.impl.RuntimeError
import me.surge.lexer.node.*
import me.surge.lexer.symbol.SymbolTable
import me.surge.lexer.token.TokenType
import me.surge.lexer.value.ListValue
import me.surge.lexer.value.NumberValue
import me.surge.lexer.value.StringValue
import me.surge.lexer.value.Value
import me.surge.lexer.value.function.FunctionValue
import me.surge.parse.RuntimeResult
import java.lang.IllegalStateException

class Interpreter {

    fun visit(node: Node, context: Context): RuntimeResult {
        val method = this::class.java.getDeclaredMethod("visit${node.javaClass.name.split(".").last()}", Node::class.java, Context::class.java)

        if (method.returnType == RuntimeResult::class.java) {
            val result = method.invoke(this, node, context) as RuntimeResult

            if (result.error != null) {
                return RuntimeResult().failure(result.error!!)
            }

            return result
        } else {
            throw IllegalStateException("Method didn't return RuntimeResult: '${"visit${node.javaClass.name.split(".").last()}"}'")
        }
    }

    fun visitBinaryOperationNode(node: Node, context: Context): RuntimeResult {
        node as BinaryOperationNode

        val result = RuntimeResult()
        val left = result.register(this.visit(node.left, context))

        if (result.shouldReturn()) {
            return result
        }

        val right = result.register(this.visit(node.right, context))

        if (result.shouldReturn()) {
            return result
        }

        left as Value
        right as Value

        val transform: Pair<Value?, Error?>

        if (node.token.matches(TokenType.KEYWORD, Constants.get("and"))) {
            transform = left.andedBy(right)
        } else if (node.token.matches(TokenType.KEYWORD, Constants.get("or"))) {
            transform = left.oredBy(right)
        } else {
            transform = when (node.token.type) {
                TokenType.PLUS -> left.addedTo(right)
                TokenType.MINUS -> left.subbedBy(right)
                TokenType.MULTIPLY -> left.multedBy(right)
                TokenType.DIVIDE -> left.divedBy(right)
                TokenType.POWER -> left.powedBy(right)
                TokenType.MODULO -> left.moduloedBy(right)
                TokenType.EQUALITY -> left.compareEquality(right)
                TokenType.INEQUALITY -> left.compareInequality(right)
                TokenType.LESS_THAN -> left.compareLessThan(right)
                TokenType.GREATER_THAN -> left.compareGreaterThan(right)
                TokenType.LESS_THAN_OR_EQUAL_TO -> left.compareLessThanOrEqualTo(right)
                TokenType.GREATER_THAN_OR_EQUAL_TO -> left.compareGreaterThanOrEqualTo(right)

                else -> {
                    // shouldn't be reached??
                    left.andedBy(right)
                }
            }
        }

        return if (transform.second != null) {
            result.failure(transform.second!!)
        } else {
            result.success(transform.first!!.setPosition(node.start, node.end))
        }
    }

    fun visitForNode(node: Node, context: Context): RuntimeResult {
        node as ForNode

        val result = RuntimeResult()
        val elements = ArrayList<Value>()

        val start = result.register(this.visit(node.startValue, context))

        if (result.shouldReturn()) {
            return result
        }

        start as NumberValue

        val end = result.register(this.visit(node.endValue, context))

        if (result.shouldReturn()) {
            return result
        }

        end as NumberValue

        val step: NumberValue = if (node.step != null) {
            val s = result.register(this.visit(node.step, context))

            if (result.shouldReturn()) {
                return result
            }

            s as NumberValue
        } else {
            NumberValue(node.name.value.toString() + "step", 1)
        }

        var i = start.value.toFloat()

        val condition: () -> Boolean = if (step.value.toInt() >= 0) {
            { i < end.value.toFloat() }
        } else {
            { i > end.value.toFloat() }
        }

        val forLoopContext = Context("for loop context", parent = context)
        forLoopContext.symbolTable = SymbolTable("anonymousfor", context.symbolTable)

        while (condition()) {
            forLoopContext.symbolTable!!.set(node.name.value as String, NumberValue(node.name.value.toString(), i), forced = true)
            i += step.value.toFloat()

            val value = result.register(this.visit(node.body, forLoopContext))

            if (result.shouldReturn() && !result.shouldContinue && !result.shouldBreak) {
                return result
            }

            if (result.shouldContinue) {
                continue
            }

            if (result.shouldBreak) {
                break
            }

            elements.add(value!!)
        }

        return result.success(if (node.shouldReturnNull) NumberValue.NULL else ListValue("<anonymous for list>", elements).setContext(forLoopContext).setPosition(node.start, node.end))
    }

    fun visitIfNode(node: Node, context: Context): RuntimeResult {
        node as IfNode

        val result = RuntimeResult()

        for (case in node.cases) {
            val conditionValue = result.register(this.visit(case.node, context))

            if (result.shouldReturn()) {
                return result
            }

            conditionValue as NumberValue

            if (conditionValue.isTrue()) {
                val newContext = Context("if", context)
                newContext.symbolTable = SymbolTable("if symbol table", context.symbolTable)

                val expressionValue = result.register(this.visit(case.token, newContext))

                if (result.shouldReturn()) {
                    return result
                }

                return result.success(if (case.shouldReturnNull) NumberValue.NULL else expressionValue)
            }
        }

        if (node.elseCase != null) {
            val newContext = Context("else", context)
            newContext.symbolTable = SymbolTable("else symbol table", context.symbolTable)

            val expressionValue = result.register(this.visit(node.elseCase.token, newContext))

            if (result.shouldReturn()) {
                return result
            }

            return result.success(if (node.elseCase.shouldReturnNull) NumberValue.NULL else expressionValue)
        }

        return result.success(NumberValue.NULL)
    }

    fun visitMethodDefineNode(node: Node, context: Context): RuntimeResult {
        node as MethodDefineNode

        val result = RuntimeResult()

        val name = if (node.name != null) node.name.value as String else "<anonymous>"
        val argumentNames = ArrayList<String>()

        for (argumentName in node.argumentTokens) {
            argumentNames.add(argumentName.value as String)
        }

        val functionValue = FunctionValue(name, node.body, argumentNames, node.shouldReturnNull)
            .setContext(context)
            .setPosition(node.start, node.end)

        if (node.name != null) {
            val error = context.symbolTable!!.set(name, functionValue, final = true, start = node.start, end = node.end, context = context, declaration = true)

            if (error != null) {
                return result.failure(error)
            }
        }

        return result.success(functionValue)
    }

    fun visitMethodCallNode(node: Node, context: Context): RuntimeResult {
        node as MethodCallNode

        val result = RuntimeResult()
        val args = ArrayList<Value>()

        var targetValue = result.register(this.visit(node.target, context))

        if (result.shouldReturn()) {
            return result
        }

        targetValue as Value

        targetValue = targetValue.clone().setPosition(node.start, node.end)

        for (argument in node.arguments) {
            val value = result.register(this.visit(argument, context))

            if (result.shouldReturn()) {
                return result
            }

            args.add(value as Value)
        }

        var returnValue = result.register(targetValue.execute(args))

        if (result.shouldReturn()) {
            return result
        }

        returnValue = returnValue!!.clone().setPosition(node.start, node.end).setContext(context)

        return result.success(returnValue)
    }

    fun visitNumberNode(node: Node, context: Context): RuntimeResult {
        node as NumberNode

        return RuntimeResult().success(
            NumberValue(node.toString(), node.token.value as Number)
                .setContext(context)
                .setPosition(
                    node.start,
                    node.end
                )
        )
    }

    fun visitStringNode(node: Node, context: Context): RuntimeResult {
        node as StringNode

        return RuntimeResult().success(StringValue(node.toString(), node.token.value as String)
            .setContext(context)
            .setPosition(node.start, node.end))
    }

    fun visitListNode(node: Node, context: Context): RuntimeResult {
        node as ListNode

        val result = RuntimeResult()
        val elements = ArrayList<Value>()

        node.elements.forEach {
            val value = result.register(this.visit(it, context))

            if (result.shouldReturn()) {
                return result
            }

            if (value is Value) {
                elements.add(value)
            }
        }

        return result.success(ListValue(node.toString(), elements).setContext(context).setPosition(node.start, node.end))
    }

    fun visitUnaryOperationNode(node: Node, context: Context): RuntimeResult {
        node as UnaryOperationNode

        val result = RuntimeResult()
        var number = result.register(this.visit(node.node, context)) as Value

        if (result.shouldReturn()) {
            return result
        }

        var error: Error? = null

        if (node.token.type == TokenType.MINUS) {
            val transform = number.multedBy(NumberValue("", -1))
            number = transform.first!!
            error = transform.second
        } else if (node.token.matches(TokenType.KEYWORD, Constants.get("not"))) {
            val transform = number.notted()
            number = transform.first!!
            error = transform.second
        }

        return if (error != null) {
            result.failure(error)
        } else {
            result.success(number.setPosition(node.start, node.end))
        }
    }

    fun visitVarAccessNode(node: Node, context: Context): RuntimeResult {
        node as VarAccessNode

        val result = RuntimeResult()
        val name = node.name.value as String
        var parent = node.parent?.value

        if (parent == null) {
            parent = ""
        }

        parent as String

        var value = context.symbolTable!!.get(name, parent)
            ?: return result.failure(RuntimeError(
                node.start,
                node.end,
                "$name is not defined!",
                context
            ))

        if (value is NumberValue) {
            value = value.clone().setPosition(node.start, node.end)
        }

        return result.success(value as Value)
    }

    fun visitVarAssignNode(node: Node, context: Context): RuntimeResult {
        node as VarAssignNode

        val result = RuntimeResult()
        val name = node.name.value as String
        var parent = node.parent?.value

        if (parent == null) {
            parent = ""
        }

        parent as String

        val value = result.register(this.visit(node.value, context))

        if (result.shouldReturn()) {
            return result
        }

        value as Value
        value.name = name

        val error = context.symbolTable?.set(name, value, final = node.final, start = node.start, end = node.end, context = context, declaration = node.declaration, parent)

        if (error != null) {
            return result.failure(error)
        }

        return result.success(value)
    }

    fun visitWhileNode(node: Node, context: Context): RuntimeResult {
        node as WhileNode

        val result = RuntimeResult()
        val elements = ArrayList<Value>()

        while (true) {
            val newContext = Context("<while context>", context)
            newContext.symbolTable = SymbolTable("<while>", parent = context.symbolTable)

            val condition = result.register(this.visit(node.condition, context))

            if (result.shouldReturn()) {
                return result
            }

            condition as NumberValue

            if (!condition.isTrue()) {
                break
            }

            val value = result.register(this.visit(node.body, newContext))

            if (result.shouldReturn() && !result.shouldContinue && !result.shouldBreak) {
                return result
            }

            if (result.shouldContinue) {
                continue
            }

            if (result.shouldBreak) {
                break
            }

            elements.add(value!!)
        }

        return result.success(if (node.shouldReturnNull) NumberValue.NULL else ListValue("<anonymous while list>", elements).setContext(context).setPosition(node.start, node.end))
    }

    fun visitReturnNode(node: Node, context: Context): RuntimeResult {
        node as ReturnNode

        val result = RuntimeResult()

        val value: Value

        if (node.toReturn != null) {
            val local = result.register(this.visit(node.toReturn, context))

            if (result.shouldReturn()) {
                return result
            }

            value = local as Value
        } else {
            value = NumberValue.NULL
        }

        return result.successReturn(value)
    }

    fun visitContinueNode(node: Node, context: Context): RuntimeResult {
        return RuntimeResult().successContinue()
    }

    fun visitBreakNode(node: Node, context: Context): RuntimeResult {
        return RuntimeResult().successBreak()
    }

}