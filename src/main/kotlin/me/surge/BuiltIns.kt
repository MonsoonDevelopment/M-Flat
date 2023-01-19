package me.surge

import me.surge.api.annotation.OverrideName
import me.surge.api.result.Failure
import me.surge.api.result.Result
import me.surge.api.result.Success
import me.surge.lexer.error.impl.RuntimeError
import me.surge.lexer.value.*
import me.surge.lexer.value.function.BaseFunctionValue
import me.surge.util.binary
import kotlin.system.exitProcess

class BuiltIns {

    @OverrideName("null")
    val NULL = 0

    @OverrideName("true")
    val TRUE = 1

    @OverrideName("false")
    val FALSE = 0

    val pi = Math.PI

    fun print(value: Value) {
        print(value.rawValue())
    }

    fun println(value: Value) {
        println(value.rawValue())
    }

    fun input(): Success {
        return Success(StringValue("<anonymous input>", readlnOrNull()!!))
    }

    fun inputMessage(text: StringValue): Success {
        print(text)
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
        return Success(NumberValue("<anonymous is number>", (value is NumberValue).binary()))
    }

    fun isString(value: Value): Result {
        return Success(NumberValue("<anonymous is string>", (value is StringValue).binary()))
    }

    fun isFunction(value: Value): Result {
        return Success(NumberValue("<anonymous is function>", (value is BaseFunctionValue).binary()))
    }

    fun isList(value: Value): Result {
        return Success(NumberValue("<anonymous is list>", (value is ListValue).binary()))
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

}