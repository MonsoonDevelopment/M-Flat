package me.surge.lexer.value.function

import me.surge.lexer.value.FunctionData
import me.surge.lexer.value.Value
import me.surge.lexer.value.ValueName
import me.surge.parse.RuntimeResult

@ValueName("built-in function")
open class BuiltInFunction(name: String, val supplier: (functionData: FunctionData) -> RuntimeResult, private val argumentNames: ArrayList<String>) : BaseFunctionValue(name) {

    override fun execute(args: ArrayList<Value>): RuntimeResult {
        val result = RuntimeResult()
        val context = this.generateContext()

        result.register(this.checkAndPopulateArguments(this.argumentNames, args, context))

        if (result.shouldReturn()) {
            return result
        }

        val value = result.register(supplier(FunctionData(start!!, end!!, context, args, this)))

        if (result.shouldReturn()) {
            return result
        }

        return result.success(value)
    }

    override fun clone(): Value {
        return BuiltInFunction(this.name, this.supplier, this.argumentNames)
            .setPosition(this.start, this.end)
            .setContext(this.context)
    }

    override fun toString(): String {
        return "<built-in function $name>"
    }

}