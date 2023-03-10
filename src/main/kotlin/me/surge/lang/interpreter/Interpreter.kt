package me.surge.lang.interpreter

import me.surge.api.Executor
import me.surge.lang.error.Error
import me.surge.lang.error.context.Context
import me.surge.lang.error.impl.RuntimeError
import me.surge.lang.lexer.token.TokenType
import me.surge.lang.node.*
import me.surge.lang.parse.RuntimeResult
import me.surge.lang.symbol.SymbolTable
import me.surge.lang.util.Constants
import me.surge.lang.value.*
import me.surge.lang.value.method.*
import me.surge.lang.value.number.FloatValue
import me.surge.lang.value.number.IntValue
import me.surge.lang.value.number.NumberValue
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

        start as NumberValue<*>

        val end = result.register(this.visit(node.endValue, context))

        if (result.shouldReturn()) {
            return result
        }

        end as NumberValue<*>

        val step: NumberValue<*> = if (node.step != null) {
            val s = result.register(this.visit(node.step, context))

            if (result.shouldReturn()) {
                return result
            }

            s as NumberValue<*>
        } else {
            IntValue(node.name.value.toString() + "step", 1)
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
            forLoopContext.symbolTable!!.set(node.name.value as String, if (step is IntValue) IntValue(node.name.value.toString(), i.toInt()) else FloatValue(node.name.value.toString(), i), SymbolTable.EntryData(immutable = true, declaration = true, start = node.start, end = node.end, context = context, forced = true))

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

            //conditionValue as BooleanValue

            val condition = conditionValue!!.isTrue()

            if (condition.second != null) {
                return result.failure(condition.second!!)
            }

            if (condition.first) {
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

        val start = IntValue(node.name.value as String + "start", 0)
        val end = IntValue(node.name.value + "end", list.elements.size)

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
        val argumentNames = arrayListOf<BaseMethodValue.Argument>()

        for (argumentName in node.argumentTokens) {
            argumentNames.add(BaseMethodValue.Argument(argumentName.token.value as String, null, true, if (argumentName.type == null) "value" else argumentName.type.value as String))
        }

        val functionValue = DefinedMethodValue(name, node.body, argumentNames, node.returnType == null, (node.returnType?.value ?: "") as String)
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

                if (index !is IntValue) {
                    return result.failure(
                        RuntimeError(
                            node.index.start,
                            node.index.end,
                            "Value isn't an int!",
                            context
                        )
                    )
                }

                val indexValue = index.value.toInt()

                if (indexValue >= returnValue.elements.size || indexValue < 0) {
                    return result.failure(
                        RuntimeError(
                            node.index.start,
                            node.index.end,
                            "Index out of bounds: $indexValue",
                            context
                        )
                    )
                }

                returnValue = returnValue.elements[indexValue]
            } else {
                return result.failure(
                    RuntimeError(
                        node.start,
                        node.end,
                        "'${returnValue!!.name}' is not indexable!",
                        context
                    )
                )
            }
        }

        if (returnValue == null) {
            returnValue = NullValue()
        }

        returnValue = returnValue.clone().setPosition(node.start, node.end).setContext(context)

        return result.success(returnValue)
    }

    fun visitNumberNode(node: Node, context: Context): RuntimeResult {
        node as NumberNode

        return RuntimeResult().success(if (node.token.type == TokenType.INT) {
            IntValue(node.toString(), node.token.value as Int)
                .setContext(context)
                .setPosition(
                    node.start,
                    node.end
                )
        } else {
            FloatValue(node.toString(), node.token.value as Float)
                .setContext(context)
                .setPosition(
                    node.start,
                    node.end
                )
        })
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

                if (endValue !is IntValue) {
                    return result.failure(RuntimeError(
                        endValue!!.start!!,
                        endValue.end!!,
                        "End wasn't an int!",
                        context
                    ))
                }

                endValue.value
            }

            if (startValue !is IntValue) {
                return result.failure(RuntimeError(
                    startValue!!.start!!,
                    startValue.end!!,
                    "Start wasn't an int!",
                    context
                ))
            }

            val start = startValue.value

            if (end == start) {
                end++
            }

            if (start < 0 || start >= value.length + 1) {
                return result.failure(
                    RuntimeError(
                        node.indexStart.start,
                        node.indexStart.end,
                        "Start index out of bounds! Got $start when the minimum index is 0, and the maximum index is ${value.length}!",
                        context
                    )
                )
            }

            if (end < 0 || end >= value.length + 1) {
                return result.failure(
                    RuntimeError(
                        node.indexStart.start,
                        node.indexStart.end,
                        "End index out of bounds! Got $end when the minimum index is 0, and the maximum index is ${value.length}!",
                        context
                    )
                )
            }

            if (end < start) {
                return result.failure(
                    RuntimeError(
                        node.indexEnd.start,
                        node.indexEnd.end,
                        "Start index is greater than end index! Got $end, when the start index is $start!",
                        context
                    )
                )
            }

            value = value.substring(start, end)
        }

        return result.success(
            StringValue(node.toString(), value)
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
            val transform = number.multedBy(IntValue("", -1))
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

        var value: Value = context.symbolTable!!.get(name, node.parent != null)
                ?: return result.failure(
                    RuntimeError(
                        node.start,
                        node.end,
                        "'$name' is not defined!",
                        context
                    )
                )

        if (node.child != null && value !is BaseMethodValue) {
            val childContext = Context(value.name, context, node.start)

            childContext.symbolTable = SymbolTable(context.symbolTable)

            value.symbols.getAll().forEach {
                childContext.symbolTable!!.set(it.identifier, it.value, SymbolTable.EntryData(immutable = false, declaration = true, start = node.start, end = node.end, context, forced = true, top = true))
            }

            node.child!!.parent = node

            val child = result.register(this.visit(node.child!!, childContext))

            if (result.shouldReturn()) {
                return result
            }

            if (node.child is VarAssignNode) {
                node.child as VarAssignNode

                val error = value.symbols.set((node.child!! as VarAssignNode).name.value as String, child!!, SymbolTable.EntryData(immutable = (node.child!! as VarAssignNode).final, declaration = (node.child!! as VarAssignNode).declaration, node.start, node.end, childContext))

                if (error != null) {
                    return result.failure(error)
                }
            }

            value = child!!
        }

        if (node.index != null) {
            if (value is ListValue) {
                val index = result.register(this.visit(node.index!!, context))

                if (result.shouldReturn()) {
                    return result
                }

                if (index !is IntValue) {
                    return result.failure(
                        RuntimeError(
                            node.index!!.start,
                            node.index!!.end,
                            "Value isn't an int!",
                            context
                        )
                    )
                }

                val indexValue = index.value

                if (indexValue >= value.elements.size || indexValue < 0) {
                    return result.failure(
                        RuntimeError(
                            node.index!!.start,
                            node.index!!.end,
                            "Index out of bounds: $indexValue",
                            context
                        )
                    )
                }

                value = value.elements[indexValue]
            } else {
                return result.failure(
                    RuntimeError(
                        node.start,
                        node.end,
                        "'${value.name}' is not indexable!",
                        context
                    )
                )
            }
        }

        value = value.clone().setPosition(node.start, node.end).setContext(context)

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

        if (original != null && original is NumberValue<*>) {
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

        if (original != null && !original.isOfType(newValue!!.identifier)) {
            return result.failure(RuntimeError(
                newValue.start!!,
                newValue.end!!,
                "'${newValue}' is not of type ${original.type()}!",
                newValue.context!!
            ))
        }

        val error = context.symbolTable?.set(name, newValue!!, SymbolTable.EntryData(immutable = node.final, declaration = node.declaration, start = node.start, end = node.end, context = context, type = original?.name ?: "value"))

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

            //condition as BooleanValue

            val conditionValue = condition!!.isTrue()

            if (conditionValue.second != null) {
                return result.failure(conditionValue.second!!)
            }

            if (!conditionValue.first) {
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

        val constructors = hashMapOf<Int, List<BaseMethodValue.Argument>>()

        node.argumentTokens.forEach { (size, tokens) ->
            if (constructors[size] == null) {
                constructors[size] = arrayListOf()
            }

            for (argumentNode in tokens) {
                val value = argumentNode.defaultValue?.let { result.register(this.visit(it, context)) }

                if (result.shouldReturn()) {
                    return result
                }

                (constructors[size] as ArrayList<BaseMethodValue.Argument>).add(BaseMethodValue.Argument(argumentNode.token.value as String, value, argumentNode.final, if (argumentNode.type == null) "value" else argumentNode.type.value as String))
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

        result.register(this.executor!!.useImplementation(node.name.value as String, node.identifier.value as String, node.start, node.end, context))

        if (result.shouldReturn()) {
            return result
        }

        return result.success(null)
    }

    fun visitEnumDefinitionNode(node: Node, context: Context): RuntimeResult {
        node as EnumDefinitionNode

        val result = RuntimeResult()

        val members = linkedMapOf<String, Value>()

        node.members.forEach { (nameToken, parameters) ->
            val args = mutableListOf<BaseMethodValue.Argument>()

            node.arguments.forEach { parameter ->
                var default: Value? = null

                if (parameter.defaultValue != null) {
                    val defaultValue = result.register(this.visit(parameter.defaultValue, context))

                    if (result.shouldReturn()) {
                        return result
                    }

                    default = defaultValue
                }

                args.add(BaseMethodValue.Argument(parameter.token.value as String, default, true))
            }

            val container = ContainerValue(nameToken.value as String, hashMapOf(Pair(args.size, args)))

            if (node.body != null) {
                container.setImplementation(node.body)
            }

            val passedParameters = arrayListOf<Value>()

            parameters.forEach { parameter ->
                val value = result.register(this.visit(parameter, context))

                if (result.shouldReturn()) {
                    return result
                }

                passedParameters.add(value!!)
            }

            val instance = result.register(container.execute(passedParameters))

            if (result.shouldReturn()) {
                return result
            }

            members[nameToken.value] = instance!!
        }

        val enum = EnumValue(node.name.value as String, members)

        context.symbolTable!!.set(node.name.value, enum, SymbolTable.EntryData(immutable = true, declaration = true, node.start, node.end, context = context, forced = true))

        return result.success(enum)
    }

}