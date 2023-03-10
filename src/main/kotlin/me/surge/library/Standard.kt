package me.surge.library

import me.surge.api.result.Failure
import me.surge.api.result.Result
import me.surge.api.result.Success
import me.surge.lang.error.impl.RuntimeError
import me.surge.lang.value.*
import me.surge.lang.value.link.JvmMethodLink
import me.surge.lang.value.number.FloatValue
import me.surge.lang.value.number.IntValue
import me.surge.lang.value.number.NumberValue
import kotlin.system.exitProcess

object Standard {

    fun print(value: Value) {
        print(value.stringValue())
    }

    fun println(value: Value) {
        println(value.stringValue())
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

    fun remove(functionData: FunctionData, list: ListValue, index: IntValue): Result {
        try {
            list.elements.removeAt(index.value.toInt())
        } catch (exception: IndexOutOfBoundsException) {
            return Failure(
                RuntimeError(
                    functionData.start!!,
                    functionData.end!!,
                    "Index out of bounds: $index",
                    functionData.context!!
                )
            )
        }

        return Success()
    }

    fun get(functionData: FunctionData, list: ListValue, index: IntValue): Result {
        val value = try {
            list.elements[index.value]
        } catch (exception: IndexOutOfBoundsException) {
            return Failure(
                RuntimeError(
                    functionData.start!!,
                    functionData.end!!,
                    "Index out of bounds: $index",
                    functionData.context!!
                )
            )
        }

        return Success(value)
    }

    fun combineLists(a: ListValue, b: ListValue) {
        a.elements.addAll(b.elements)
    }

    fun isNumber(value: Value): Result {
        return Success(BooleanValue("<anonymous is number>", value is NumberValue<*>))
    }

    fun isString(value: Value): Result {
        return Success(BooleanValue("<anonymous is string>", value is StringValue))
    }

    fun isMethod(value: Value): Result {
        return Success(BooleanValue("<anonymous is method>", value is  JvmMethodLink))
    }

    fun isList(value: Value): Result {
        return Success(BooleanValue("<anonymous is list>", value is ListValue))
    }

    fun checkType(functionData: FunctionData, instance: InstanceValue, parent: Value): Result {
        return Success(BooleanValue("<anonymous check>", instance.parent == parent))
    }

    fun stringToNumber(value: StringValue): Result {
        val string = value.stringValue()

        return if (string.contains('.')) {
            Success(FloatValue("<anonymous cast>", string.toFloat()))
        } else {
            Success(IntValue("<anonymous cast>", string.toInt()))
        }
    }

    fun stringToBool(value: StringValue): Result {
        return Success(BooleanValue("<anonymous cast>", value.stringValue().lowercase() == "true"))
    }

    fun delete(functionData: FunctionData, name: Value): Result {
        val error = functionData.context!!.symbolTable!!.removeGlobally(name.name, start = functionData.start, end = functionData.end, context = functionData.context)

        if (error != null) {
            return Failure(error)
        }

        return Success()
    }

    fun exit(status: IntValue): Result {
        exitProcess(status.value)
    }

    fun type(value: Value): Result {
        return Success(StringValue("anonymous", value.name))
    }

}