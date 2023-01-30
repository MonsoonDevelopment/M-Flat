package me.surge.lexer.value

import me.surge.interpreter.Interpreter
import me.surge.lexer.error.context.Context
import me.surge.lexer.error.impl.RuntimeError
import me.surge.lexer.node.ListNode
import me.surge.lexer.node.Node
import me.surge.lexer.symbol.SymbolTable
import me.surge.lexer.value.function.BaseFunctionValue
import me.surge.parse.RuntimeResult

@ValueName("container")
class ContainerValue(name: String, val constructors: HashMap<Int, ArrayList<String>>) : BaseFunctionValue(name) {

    private var body: Node? = null
    var implement: ((Node?, Context, Interpreter) -> Unit)? = null

    override fun execute(args: ArrayList<Value>): RuntimeResult {
        val functionResult = RuntimeResult()

        if (this.context == null) {
            this.context = this.generateContext()
        }

        if (implement != null) {
            implement!!(body, this.context!!, Interpreter(null))
        }

        val table = SymbolTable(this.context?.symbolTable)

        var argNames: ArrayList<String> = arrayListOf()

        run loop@ {
            constructors.forEach { (_, argumentNames) ->
                val res = this.checkArguments(argumentNames, args)

                if (!res.shouldReturn()) {
                    argNames = argumentNames
                    return@loop
                }
            }
        }

        functionResult.register(this.checkAndPopulateArguments(argNames, args, this.context!!))

        if (functionResult.shouldReturn()) {
            return functionResult
        }

        args.forEachIndexed { index, value ->
            table.set(argNames[index], value, SymbolTable.EntryData(immutable = true, declaration = true, this.start, this.end, this.context!!, forced = true))
        }

        constructors.forEach { (_, argumentNames) ->
            argumentNames.forEach { name ->
                if (!this.context!!.symbolTable!!.symbols.any { it.identifier == name }) {
                    table.set(name, NullValue(), SymbolTable.EntryData(immutable = true, declaration = true, this.start, this.end, this.context!!, forced = true))
                }
            }
        }

        return functionResult.success(ContainerInstanceValue(name, table, this))
    }

    fun setImplementation(body: Node) {
        body as ListNode

        this.body = body

        implement = { body, context, interpreter ->
            val implementationContext = Context(this.name, context).createChildSymbolTable()

            implementationContext.symbolTable!!.set(
                "this",
                ContainerInstanceValue(this.name, implementationContext.symbolTable!!, this),
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

    override fun rawValue(): String {
        return "<container $name>"
    }

}