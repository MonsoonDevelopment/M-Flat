package me.surge.interpreter

import me.surge.util.Constants
import me.surge.api.Executor
import me.surge.lexer.error.Error
import me.surge.lexer.error.context.Context
import me.surge.lexer.error.impl.RuntimeError
import me.surge.lexer.node.*
import me.surge.lexer.symbol.SymbolTable
import me.surge.lexer.token.TokenType
import me.surge.lexer.value.*
import me.surge.lexer.value.function.BaseFunctionValue
import me.surge.lexer.value.function.FunctionValue
import me.surge.parse.RuntimeResult
import java.lang.IllegalStateException

class Interpreter(val executor: Executor? = null) {

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
        forLoopContext.symbolTable = SymbolTable(context.symbolTable)

        while (condition()) {
            forLoopContext.symbolTable!!.set(node.name.value as String, NumberValue(node.name.value.toString(), i), SymbolTable.EntryData(immutable = true, declaration = true, start = node.start, end = node.end, context = context, forced = true))

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

        return result.success(if (node.shouldReturnNull) NullValue() else ListValue("<anonymous for list>", elements).setContext(forLoopContext).setPosition(node.start, node.end))
    }

    fun visitIfNode(node: Node, context: Context): RuntimeResult {
        node as IfNode

        val result = RuntimeResult()

        for (case in node.cases) {
            val conditionValue = result.register(this.visit(case.node, context))

            if (result.shouldReturn()) {
                return result
            }

            conditionValue as BooleanValue

            if (conditionValue.isTrue()) {
                val newContext = Context("if", context)
                newContext.symbolTable = SymbolTable(context.symbolTable)

                val expressionValue = result.register(this.visit(case.token, newContext))

                if (result.shouldReturn()) {
                    return result
                }

                return result.success(if (case.shouldReturnNull) NullValue() else expressionValue)
            }
        }

        if (node.elseCase != null) {
            val newContext = Context("else", context)
            newContext.symbolTable = SymbolTable(context.symbolTable)

            val expressionValue = result.register(this.visit(node.elseCase.token, newContext))

            if (result.shouldReturn()) {
                return result
            }

            return result.success(if (node.elseCase.shouldReturnNull) NullValue() else expressionValue)
        }

        return result.success(NullValue())
    }

    fun visitIterationNode(node: Node, context: Context): RuntimeResult {
        node as IterationNode

        val result = RuntimeResult()
        val elements = ArrayList<Value>()

        val list = context.symbolTable!!.get(node.list.value as String) as ListValue

        val start = NumberValue(node.name.value as String + "start", 0)
        val end = NumberValue(node.name.value + "end", list.elements.size)

        var i = start.value.toFloat()

        val condition: () -> Boolean = { i < end.value.toFloat() }

        val iterationContext = Context("iteration loop context", parent = context)
        iterationContext.symbolTable = SymbolTable(context.symbolTable)

        while (condition()) {
            val error = iterationContext.symbolTable!!.set(
                node.name.value,
                list.elements[i.toInt()],
                SymbolTable.EntryData(
                    immutable = true,
                    declaration = true,
                    start = node.start,
                    end = node.end,
                    context = context,
                    forced = true
                )
            )

            if (error != null) {
                return result.failure(error)
            }

            i += 1

            val value = result.register(this.visit(node.body, iterationContext))

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

        return result.success(if (node.shouldReturnNull) NullValue() else ListValue("<anonymous iteration list>", elements).setContext(iterationContext).setPosition(node.start, node.end))
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
            val error = context.symbolTable!!.set(name, functionValue, SymbolTable.EntryData(immutable = true, declaration = true, start = node.start, end = node.end, context = context))

            if (error != null) {
                return result.failure(error)
            }
        }

        return result.success(functionValue)
    }

    fun visitMethodCallNode(node: Node, context: Context): RuntimeResult {
        node as MethodCallNode
        node.target as VarAccessNode

        val result = RuntimeResult()
        val args = ArrayList<Value>()

        for (argument in node.arguments) {
            val value = result.register(this.visit(argument, context))

            if (result.shouldReturn()) {
                return result
            }

            args.add(value as Value)
        }

        var targetValue = result.register(this.visit(node.target, context))

        if (result.shouldReturn()) {
            return result
        }

        targetValue as Value

        targetValue = targetValue.clone().setPosition(node.start, node.end)

        targetValue.setContext(context)

        var returnValue = result.register(targetValue.execute(args))

        if (result.shouldReturn()) {
            return result
        }

        if (node.child != null) {
            returnValue = result.register(this.visit(node.child!!, context))
        }

        if (node.index != null) {
            if (returnValue is ListValue) {
                val index = result.register(this.visit(node.index, context))

                if (result.shouldReturn()) {
                    return result
                }

                if (index !is NumberValue) {
                    return result.failure(RuntimeError(
                        node.index.start,
                        node.index.end,
                        "Value isn't a number!",
                        context
                    ))
                }

                val indexValue = index.value.toInt()

                if (indexValue >= returnValue.elements.size || indexValue < 0) {
                    return result.failure(RuntimeError(
                        node.index.start,
                        node.index.end,
                        "Index out of bounds: $indexValue",
                        context
                    ))
                }

                returnValue = returnValue.elements[indexValue]
            } else {
                return result.failure(RuntimeError(
                    node.start,
                    node.end,
                    "'${returnValue!!.name}' is not indexable!",
                    context
                ))
            }
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

        val result = RuntimeResult()

        var value = node.token.value as String

        if (node.indexStart != null) {
            val startValue = result.register(this.visit(node.indexStart, context))

            if (result.shouldReturn()) {
                return result
            }

            var end: Int = if (node.indexEnd is EndNode) {
                value.length
            } else {
                val endValue = result.register(this.visit(node.indexEnd!!, context))

                if (result.shouldReturn()) {
                    return result
                }

                (endValue as NumberValue).value.toInt()
            }

            val start = (startValue as NumberValue).value.toInt()

            if (end == start) {
                end++
            }

            if (start < 0 || start >= value.length + 1) {
                return result.failure(RuntimeError(
                    node.indexStart.start,
                    node.indexStart.end,
                    "Start index out of bounds! Got $start when the minimum index is 0, and the maximum index is ${value.length}!",
                    context
                ))
            }

            if (end < 0 || end >= value.length + 1) {
                return result.failure(RuntimeError(
                    node.indexStart.start,
                    node.indexStart.end,
                    "End index out of bounds! Got $end when the minimum index is 0, and the maximum index is ${value.length}!",
                    context
                ))
            }

            if (end < start) {
                return result.failure(RuntimeError(
                    node.indexEnd.start,
                    node.indexEnd.end,
                    "Start index is greater than end index! Got $end, when the start index is $start!",
                    context
                ))
            }

            value = value.substring(start, end)
        }

        return result.success(StringValue(node.toString(), value)
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

        var value: Value? = context.symbolTable!!.get(name)
                ?: return result.failure(RuntimeError(
                    node.start,
                    node.end,
                    "'$name' is not defined!",
                    context
                ))

        if (node.child != null && value !is BaseFunctionValue) {
            if (value !is ContainerInstanceValue) {
                return result.failure(RuntimeError(
                    node.start,
                    node.end,
                    "'${value!!.name}' is not a container, it is '${value.rawName}'!",
                    context
                ))
            }

            val childContext = Context(value.name, context, node.start)

            childContext.symbolTable = SymbolTable(context.symbolTable)

            value.value.getAll().forEach {
                childContext.symbolTable!!.set(it.identifier, it.value, SymbolTable.EntryData(immutable = false, declaration = true, start = node.start, end = node.end, context, forced = true))
            }

            val child = result.register(this.visit(node.child!!, childContext))

            if (result.error != null) {
                return result
            }

            if (node.child is VarAssignNode) {
                node.child as VarAssignNode
                (value.value as SymbolTable).set((node.child!! as VarAssignNode).name.value as String, child!!, SymbolTable.EntryData(immutable = (node.child!! as VarAssignNode).final, declaration = (node.child!! as VarAssignNode).declaration, node.start, node.end, childContext))
            }

            value = child
        } else {
            value = context.symbolTable!!.get(name) ?: return result.failure(RuntimeError(
                node.start,
                node.end,
                "'$name' is not defined!",
                context
            ))
        }

        if (node.index != null) {
            if (value is ListValue) {
                val index = result.register(this.visit(node.index!!, context))

                if (result.shouldReturn()) {
                    return result
                }

                if (index !is NumberValue) {
                    return result.failure(RuntimeError(
                        node.index!!.start,
                        node.index!!.end,
                        "Value isn't a number!",
                        context
                    ))
                }

                val indexValue = index.value.toInt()

                if (indexValue >= value.elements.size || indexValue < 0) {
                    return result.failure(RuntimeError(
                        node.index!!.start,
                        node.index!!.end,
                        "Index out of bounds: $indexValue",
                        context
                    ))
                }

                value = value.elements[indexValue]
            } else {
                return result.failure(RuntimeError(
                    node.start,
                    node.end,
                    "'${value!!.name}' is not indexable!",
                    context
                ))
            }
        }

        value = value!!.clone().setPosition(node.start, node.end).setContext(context)

        return result.success(value)
    }

    fun visitVarAssignNode(node: Node, context: Context): RuntimeResult {
        node as VarAssignNode

        val result = RuntimeResult()

        val name = node.name.value as String

        val value = result.register(this.visit(node.value, context))

        if (result.shouldReturn()) {
            return result
        }

        value as Value
        value.name = name

        val original = context.symbolTable?.get(name)
        var newValue = value

        if (original != null && original is NumberValue) {
            when (node.mutate) {
                null -> {}

                TokenType.ADD -> {
                    newValue = original.addedTo(newValue).first
                }

                TokenType.SUBTRACT_BY -> {
                    newValue = original.subbedBy(newValue).first
                }

                TokenType.MULTIPLY_BY -> {
                    newValue = original.multedBy(newValue).first
                }

                TokenType.DIVIDE_BY -> {
                    newValue = original.divedBy(newValue).first
                }

                else -> {}
            }
        }

        val error = context.symbolTable?.set(name, newValue!!, SymbolTable.EntryData(immutable = node.final, declaration = node.declaration, start = node.start, end = node.end, context = context))

        if (error != null) {
            return result.failure(error)
        }

        return result.success(newValue)
    }

    fun visitWhileNode(node: Node, context: Context): RuntimeResult {
        node as WhileNode

        val result = RuntimeResult()
        val elements = ArrayList<Value>()

        while (true) {
            val newContext = Context("<while context>", context)
            newContext.symbolTable = SymbolTable(parent = context.symbolTable)

            val condition = result.register(this.visit(node.condition, context))

            if (result.shouldReturn()) {
                return result
            }

            condition as BooleanValue

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

        return result.success(if (node.shouldReturnNull) NullValue() else ListValue("<anonymous while list>", elements).setContext(context).setPosition(node.start, node.end))
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
            value = NullValue()
        }

        return result.successReturn(value)
    }

    fun visitContinueNode(node: Node, context: Context): RuntimeResult {
        return RuntimeResult().successContinue()
    }

    fun visitBreakNode(node: Node, context: Context): RuntimeResult {
        return RuntimeResult().successBreak()
    }

    fun visitContainerDefinitionNode(node: Node, context: Context): RuntimeResult {
        node as ContainerDefinitionNode

        val result = RuntimeResult()

        val constructors = hashMapOf<Int, ArrayList<String>>()

        node.argumentTokens.forEach { (size, tokens) ->
            if (constructors[size] == null) {
                constructors[size] = arrayListOf()
            }

            for (name in tokens) {
                constructors[size]!!.add(name.value as String)
            }
        }

        val functionValue = ContainerValue(node.name.value as String, constructors)

        if (node.body != null) {
            functionValue.setImplementation(node.body)
        }

        context.symbolTable!!.set(node.name.value, functionValue, SymbolTable.EntryData(immutable = true, declaration = true, node.start, node.end, context = context, forced = true))

        return result.success(functionValue)
    }

    fun visitImportNode(node: Node, context: Context): RuntimeResult {
        node as ImportNode

        val result = RuntimeResult()

        result.register(this.executor!!.useImplementation(node.name.value as String, node.start, node.end, context))

        if (result.shouldReturn()) {
            return result
        }

        return result.success(null)
    }

}