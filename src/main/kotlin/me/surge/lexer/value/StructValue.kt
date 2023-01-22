package me.surge.lexer.value

import me.surge.lexer.symbol.SymbolTable
import me.surge.lexer.value.function.BaseFunctionValue
import me.surge.parse.RuntimeResult

@ValueName("struct")
class StructValue(name: String, val argumentNames: ArrayList<String>) : BaseFunctionValue(name) {

    override fun execute(args: ArrayList<Value>): RuntimeResult {
        val functionResult = RuntimeResult()

        val table = SymbolTable()
        val functionContext = this.generateContext()

        functionResult.register(this.checkAndPopulateArguments(argumentNames, args, functionContext))

        if (functionResult.shouldReturn()) {
            return functionResult
        }

        args.forEachIndexed { index, value ->
            table.set(argumentNames[index], value, SymbolTable.EntryData(immutable = true, declaration = true, this.start, this.end, functionContext))
        }

        return functionResult.success(ContainerValue(name, table))
    }

    override fun clone(): Value {
        return this
    }

}