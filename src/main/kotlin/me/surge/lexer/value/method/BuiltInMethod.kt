package me.surge.lexer.value.method

import me.surge.lexer.error.context.Context
import me.surge.lexer.value.FunctionData
import me.surge.lexer.value.Value
import me.surge.parse.RuntimeResult

class BuiltInMethod(identifier: String, val invoke: (functionData: FunctionData) -> RuntimeResult, private val argumentNames: List<Argument>) : BaseMethodValue(identifier, "JVM METHOD") {

    override fun execute(args: List<Value>, context: Context): RuntimeResult {
        val result = RuntimeResult()

        result.register(this.checkAndPopulateArguments(this.argumentNames, args, context))

        if (result.shouldReturn()) {
            return result
        }

        val value = result.register(invoke(FunctionData(this.start!!, this.end!!, context, args, this)))

        if (result.shouldReturn()) {
            return result
        }

        return result.success(value)
    }

    override fun execute(args: List<Value>): RuntimeResult {
        return execute(args, this.generateContext())
    }

    override fun clone(): Value {
        return BuiltInMethod(this.identifier, this.invoke, this.argumentNames)
            .setPosition(this.start, this.end)
            .setContext(this.context)
    }

    override fun toString(): String {
        return stringValue()
    }

    override fun stringValue(): String {
        return "<JVM METHOD [$identifier]>"
    }

}