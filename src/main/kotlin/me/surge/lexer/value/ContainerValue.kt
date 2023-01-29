package me.surge.lexer.value

import me.surge.interpreter.Interpreter
import me.surge.lexer.error.context.Context
import me.surge.lexer.node.ListNode
import me.surge.lexer.node.Node
import me.surge.lexer.symbol.SymbolTable
import me.surge.lexer.value.function.BaseFunctionValue
import me.surge.parse.RuntimeResult

@ValueName("container")
class ContainerValue(name: String, val argumentNames: ArrayList<String>) : BaseFunctionValue(name) {

    private var body: Node? = null
    private var implement: ((Node?, Context, Interpreter) -> Unit)? = null

    override fun execute(args: ArrayList<Value>): RuntimeResult {
        val functionResult = RuntimeResult()

        if (this.context == null) {
            this.context = this.generateContext()
        }

        if (implement != null) {
            implement!!(body, this.context!!, Interpreter(null))
        }

        val table = SymbolTable(this.context?.symbolTable)

        functionResult.register(this.checkAndPopulateArguments(argumentNames, args, this.context!!))

        if (functionResult.shouldReturn()) {
            return functionResult
        }

        args.forEachIndexed { index, value ->
            table.set(argumentNames[index], value, SymbolTable.EntryData(immutable = true, declaration = true, this.start, this.end, this.context!!, forced = true))
        }

        return functionResult.success(ContainerInstanceValue(name, table))
    }

    fun setImplementation(body: Node) {
        body as ListNode

        this.body = body

        implement = { body, context, interpreter ->
            val implementationContext = Context(this.name, context).createChildSymbolTable()

            implementationContext.symbolTable!!.set(
                "this",
                ContainerInstanceValue(this.name, implementationContext.symbolTable!!),
                SymbolTable.EntryData(
                    immutable = true,
                    declaration = true,
                    this.start,
                    this.end,
                    implementationContext,
                    forced = true
                )
            )

            interpreter.visit(body!!, implementationContext)

            this.context = implementationContext
        }
    }

    override fun clone(): Value {
        return this
    }

}