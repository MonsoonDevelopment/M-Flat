package me.surge.lexer.value.function

import me.surge.interpreter.Interpreter
import me.surge.lexer.node.Node
import me.surge.lexer.value.NumberValue
import me.surge.lexer.value.Value
import me.surge.lexer.value.ValueName
import me.surge.parse.RuntimeResult

@ValueName("function")
class FunctionValue(name: String = "<anonymous>", val body: Node, val argumentNames: ArrayList<String>, val shouldAutoReturn: Boolean) : BaseFunctionValue(name) {

    override fun execute(args: ArrayList<Value>): RuntimeResult {
        val result = RuntimeResult()
        val interpreter = Interpreter()
        val context = this.generateContext()

        result.register(this.checkAndPopulateArguments(this.argumentNames, args, context))

        if (result.shouldReturn()) {
            return result
        }

        val value = result.register(interpreter.visit(this.body, context))

        if (result.shouldReturn() && result.returnValue == null) {
            return result
        }

        val three = NumberValue.NULL

        return result.success((if (shouldAutoReturn) value else null) ?: (result.returnValue ?: three))
    }

    override fun clone(): Value {
        return FunctionValue(this.name, this.body, this.argumentNames, this.shouldAutoReturn)
            .setPosition(this.start, this.end)
            .setContext(this.context)
    }

    override fun toString(): String {
        return "<function $name>"
    }

}