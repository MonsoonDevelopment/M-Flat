package me.surge.lexer.value.function

import me.surge.lexer.error.context.Context
import me.surge.lexer.error.impl.RuntimeError
import me.surge.lexer.symbol.SymbolTable
import me.surge.lexer.value.Value
import me.surge.lexer.value.ValueName
import me.surge.parse.RuntimeResult

@ValueName("function")
open class BaseFunctionValue(name: String = "<anonymous>") : Value(name) {

    fun generateContext(): Context {
        val new = Context(this.name, this.context, this.start)
        new.symbolTable = SymbolTable("<base function>", parent = new.parent?.symbolTable)
        return new
    }

    fun checkArguments(argumentNames: ArrayList<String>, arguments: ArrayList<Value>): RuntimeResult {
        val result = RuntimeResult()

        if (arguments.size > argumentNames.size) {
            return result.failure(RuntimeError(
                this.start!!,
                this.end!!,
                "Too many arguments passed into $this",
                this.context!!
            ))
        } else if (arguments.size < argumentNames.size) {
            return result.failure(RuntimeError(
                this.start!!,
                this.end!!,
                "Too little arguments passed into $this",
                this.context!!
            ))
        }

        return result.success(null)
    }

    fun populateArguments(argumentNames: ArrayList<String>, arguments: ArrayList<Value>, context: Context) {
        arguments.forEachIndexed { index, value ->
            run {
                val name = argumentNames[index]
                val argumentValue = arguments[index]
                argumentValue.setContext(context)
                context.symbolTable!!.set(name, argumentValue)
            }
        }
    }

    fun checkAndPopulateArguments(argumentNames: ArrayList<String>, arguments: ArrayList<Value>, context: Context): RuntimeResult {
        val result = RuntimeResult()

        result.register(this.checkArguments(argumentNames, arguments))

        if (result.error != null) {
            return result
        }

        this.populateArguments(argumentNames, arguments, context)

        return result.success(null)
    }

}