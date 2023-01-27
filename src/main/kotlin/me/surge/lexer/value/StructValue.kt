package me.surge.lexer.value

import me.surge.interpreter.Interpreter
import me.surge.lexer.error.context.Context
import me.surge.lexer.node.ListNode
import me.surge.lexer.node.Node
import me.surge.lexer.symbol.SymbolTable
import me.surge.lexer.value.function.BaseFunctionValue
import me.surge.parse.RuntimeResult

@ValueName("struct")
class StructValue(name: String, val argumentNames: ArrayList<String>) : BaseFunctionValue(name) {

    override fun execute(args: ArrayList<Value>): RuntimeResult {
        val functionResult = RuntimeResult()

        if (this.context == null) {
            this.context = this.generateContext()
        }

        val table = SymbolTable(this.context?.symbolTable)

        functionResult.register(this.checkAndPopulateArguments(argumentNames, args, this.context!!))

        if (functionResult.shouldReturn()) {
            return functionResult
        }

        args.forEachIndexed { index, value ->
            table.set(argumentNames[index], value, SymbolTable.EntryData(immutable = true, declaration = true, this.start, this.end, this.context!!))
        }

        return functionResult.success(ContainerValue(name, table))
    }

    fun setImplementation(body: Node, context: Context, result: RuntimeResult, interpreter: Interpreter): RuntimeResult {
        body as ListNode

        val implementationContext = Context(this.name, context).createChildSymbolTable()
        implementationContext.symbolTable!!.set("this", ContainerValue(this.name, implementationContext.symbolTable), SymbolTable.EntryData(immutable = true, declaration = true, this.start, this.end, implementationContext, forced = true))

        result.register(interpreter.visit(body, implementationContext))

        if (result.error != null) {
            return result
        }

        this.context = implementationContext

        return result
    }

    override fun clone(): Value {
        return this
    }

}