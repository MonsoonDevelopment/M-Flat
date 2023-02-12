package me.surge.lang.value.link

import me.surge.lang.error.context.Context
import me.surge.lang.parse.RuntimeResult
import me.surge.lang.value.FunctionData
import me.surge.lang.value.Value
import me.surge.lang.value.method.BaseMethodValue

class JvmLinkMethod(identifier: String, val invoke: (functionData: FunctionData) -> RuntimeResult, val argumentNames: List<Argument>) : BaseMethodValue(identifier, "JVM METHOD") {

    override fun execute(args: List<Value>, context: Context): RuntimeResult {
        val result = RuntimeResult()

        result.register(this.checkAndPopulateArguments(this.argumentNames, args, context))

        if (result.shouldReturn()) {
            return result
        }

        val value = result.register(invoke(FunctionData(this.start, this.end, context, args, this)))

        if (result.shouldReturn()) {
            return result
        }

        return result.success(value)
    }

    override fun execute(args: List<Value>): RuntimeResult {
        return execute(args, this.generateContext())
    }

    override fun clone(): Value {
        return JvmLinkMethod(this.identifier, this.invoke, this.argumentNames)
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