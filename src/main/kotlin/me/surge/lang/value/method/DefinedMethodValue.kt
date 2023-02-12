package me.surge.lang.value.method

import me.surge.lang.interpreter.Interpreter
import me.surge.lang.error.context.Context
import me.surge.lang.node.Node
import me.surge.lang.parse.RuntimeResult
import me.surge.lang.value.NullValue
import me.surge.lang.value.Value

class DefinedMethodValue(identifier: String, val body: Node?, private val argumentNames: List<Argument>, val shouldAutoReturn: Boolean) : BaseMethodValue(identifier, "defined method") {

    override fun execute(args: List<Value>, context: Context): RuntimeResult {
        val result = RuntimeResult()
        val interpreter = Interpreter()

        result.register(this.checkAndPopulateArguments(this.argumentNames, args, context))

        if (result.shouldReturn()) {
            return result
        }

        if (this.body != null) {
            val value = result.register(interpreter.visit(this.body, context))

            if (result.shouldReturn() && result.returnValue == null) {
                return result
            }

            return result.success((if (shouldAutoReturn) value else null) ?: (result.returnValue ?: NullValue()))
        }

        return result.success(NullValue())
    }

    override fun execute(args: List<Value>): RuntimeResult {
        return execute(args, this.generateContext())
    }

    override fun clone(): Value {
        return DefinedMethodValue(this.identifier, this.body, this.argumentNames, this.shouldAutoReturn)
            .setPosition(this.start, this.end)
            .setContext(this.context)
    }

    override fun stringValue(): String {
        return "<method $identifier#(${this.argumentNames})>"
    }

}