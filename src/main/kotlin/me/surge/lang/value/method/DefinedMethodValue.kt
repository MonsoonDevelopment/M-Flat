package me.surge.lang.value.method

import me.surge.lang.interpreter.Interpreter
import me.surge.lang.error.context.Context
import me.surge.lang.error.impl.RuntimeError
import me.surge.lang.node.Node
import me.surge.lang.parse.RuntimeResult
import me.surge.lang.value.NullValue
import me.surge.lang.value.Value

class DefinedMethodValue(identifier: String, val body: Node?, private val argumentNames: List<Argument>, val shouldAutoReturn: Boolean, val returnType: String) : BaseMethodValue(identifier, "defined method") {

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

            val returnValue = (if (shouldAutoReturn) value else null) ?: (result.returnValue ?: NullValue())

            if (returnType != "") {
                if (!returnValue.isOfType(returnType)) {
                    return result.failure(RuntimeError(
                        returnValue.start!!,
                        returnValue.end!!,
                        "Returned value ($returnValue) was not of type $returnType",
                        context
                    ))
                }
            }

            return result.success(returnValue)
        }

        return result.success(NullValue())
    }

    override fun execute(args: List<Value>): RuntimeResult {
        return execute(args, this.generateContext())
    }

    override fun clone(): Value {
        return DefinedMethodValue(this.identifier, this.body, this.argumentNames, this.shouldAutoReturn, this.returnType)
            .setPosition(this.start, this.end)
            .setContext(this.context)
    }

    override fun stringValue(): String {
        return "<method $identifier#(${this.argumentNames})>"
    }

}