package me.surge.library

import me.surge.api.result.Failure
import me.surge.api.result.Result
import me.surge.api.result.Success
import me.surge.lexer.error.impl.RuntimeError
import me.surge.lexer.value.*
import me.surge.lexer.value.function.BaseFunctionValue
import me.surge.util.binary
import kotlin.system.exitProcess

object Standard {

    fun print(value: Value) {
        print(value.rawValue())
    }

    fun println(value: Value) {
        println(value.rawValue())
    }

    fun input(): Success {
        return Success(StringValue("<anonymous input>", readlnOrNull()!!))
    }

    fun inputMessage(message: StringValue): Success {
        print(message)
        return input()
    }

    fun add(list: ListValue, value: Value) {
        list.elements.add(value)
    }

    fun remove(functionData: FunctionData, list: ListValue, index: NumberValue): Result {
        try {
            list.elements.removeAt(index.value.toInt())
        } catch (exception: IndexOutOfBoundsException) {
            return Failure(
                RuntimeError(
                functionData.start,
                functionData.end,
                "Index out of bounds: $index",
                functionData.context
            ))
        }

        return Success()
    }

    fun get(functionData: FunctionData, list: ListValue, index: NumberValue): Result {
        val value = try {
            list.elements[index.value.toInt()]
        } catch (exception: IndexOutOfBoundsException) {
            return Failure(
                RuntimeError(
                functionData.start,
                functionData.end,
                "Index out of bounds: $index",
                functionData.context
            ))
        }

        return Success(value)
    }

    fun combineLists(a: ListValue, b: ListValue) {
        a.elements.addAll(b.elements)
    }

    fun isNumber(value: Value): Result {
        return Success(BooleanValue("<anonymous is number>", value is NumberValue))
    }

    fun isString(value: Value): Result {
        return Success(BooleanValue("<anonymous is string>", value is StringValue))
    }

    fun isMethod(value: Value): Result {
        return Success(BooleanValue("<anonymous is method>", value is BaseFunctionValue))
    }

    fun isList(value: Value): Result {
        return Success(BooleanValue("<anonymous is list>", value is ListValue))
    }

    fun matchesContainer(functionData: FunctionData, containerValue: Value, parent: Value): Result {
        if (containerValue !is ContainerInstanceValue) {
            return Failure(RuntimeError(
                functionData.start,
                functionData.end,
                "First argument of 'matchesContainer' must be an instance!",
                functionData.context
            ))
        }

        if (parent !is ContainerValue) {
            return Failure(RuntimeError(
                functionData.start,
                functionData.end,
                "Second argument of 'matchesContainer' must be a container!",
                functionData.context
            ))
        }

        return Success(BooleanValue("<anonymous matches>", containerValue.parent!!.name == parent.name))
    }

    fun stringToNumber(value: Value): Result {
        return Success(NumberValue("<anonymous cast>", value.rawValue().toFloat()))
    }

    fun stringToBool(value: Value): Result {
        return Success(BooleanValue("<anonymous cast>", value.rawValue().lowercase() == "true"))
    }

    fun delete(functionData: FunctionData, name: Value): Result {
        val error = functionData.context.symbolTable!!.removeGlobally(name.name, start = functionData.start, end = functionData.end, context = functionData.context)

        if (error != null) {
            return Failure(error)
        }

        return Success()
    }

    fun exit(status: NumberValue): Result {
        exitProcess(status.value.toInt())
    }

    fun type(value: Value): Result {
        return Success(StringValue("anonymous", value.rawName))
    }

    fun equalsIgnoreCase(a: StringValue, b: StringValue): Result {
        return Success(BooleanValue("anonymous", a.rawValue().lowercase() == b.rawValue().lowercase()))
    }

}