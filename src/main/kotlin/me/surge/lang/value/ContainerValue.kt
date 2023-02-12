package me.surge.lang.value

import me.surge.lang.interpreter.Interpreter
import me.surge.lang.error.context.Context
import me.surge.lang.node.ListNode
import me.surge.lang.node.Node
import me.surge.lang.parse.RuntimeResult
import me.surge.lang.symbol.SymbolTable
import me.surge.lang.value.method.BaseMethodValue

open class ContainerValue(identifier: String, val constructors: HashMap<Int, List<Argument>>, name: String = "container") : BaseMethodValue(identifier, name) {

    private var body: Node? = null
    var implement: ((Node?, Context, Interpreter) -> Unit)? = null

    override fun execute(args: List<Value>): RuntimeResult {
        val functionResult = RuntimeResult()

        if (this.context == null) {
            this.context = this.generateContext()
        }

        if (implement != null) {
            implement!!(body, this.context!!, Interpreter(null))
        }

        val table = SymbolTable(this.context?.symbolTable)

        var argNames: List<Argument> = arrayListOf()

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
            table.set(argNames[index].name, value, SymbolTable.EntryData(immutable = true, declaration = true, this.start, this.end, this.context!!, forced = true))
        }

        constructors.forEach { (_, argumentNames) ->
            argumentNames.forEach { name ->
                if (!this.context!!.symbolTable!!.symbols.any { it.identifier == name.name }) {
                    this.context!!.symbolTable!!.set(name.name, NullValue(), SymbolTable.EntryData(immutable = true, declaration = true, this.start, this.end, this.context!!, forced = true))
                }
            }
        }

        val init = this.context!!.symbolTable!!.get("init") as BaseMethodValue?

        if (init != null) {
            val check = init.checkArguments(arrayListOf(), arrayListOf())

            if (!check.shouldReturn()) {
                functionResult.register(init.execute(arrayListOf(), context!!))

                if (functionResult.shouldReturn()) {
                    return functionResult
                }
            }
        }

        return functionResult.success(InstanceValue(this.identifier, table))
    }

    fun setImplementation(body: Node) {
        body as ListNode

        this.body = body

        implement = { body, context, interpreter ->
            val implementationContext = Context(this.name, context).createChildSymbolTable()

            implementationContext.symbolTable!!.set(
                "this",
                InstanceValue(this.identifier, implementationContext.symbolTable!!),
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

    override fun toString(): String {
        return stringValue()
    }

    override fun stringValue(): String {
        return "<container $identifier>"
    }

}