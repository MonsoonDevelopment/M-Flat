package me.surge.lang.value.method

import me.surge.lang.error.context.Context
import me.surge.lang.error.impl.RuntimeError
import me.surge.lang.parse.RuntimeResult
import me.surge.lang.symbol.SymbolTable
import me.surge.lang.value.Value

open class BaseMethodValue(identifier: String = "<anonymous>", name: String) : Value(identifier, name) {

    fun generateContext(): Context {
        val new = Context(this.name, this.context, this.start)
        new.symbolTable = SymbolTable(parent = new.parent?.symbolTable)
        return new
    }

    /**
     * [argumentNames] contains the argument names and whether they are required or not
     * [arguments] provides the given arguments
     */
    fun checkArguments(argumentNames: List<Argument>, arguments: List<Value>): RuntimeResult {
        val result = RuntimeResult()

        if (arguments.size > argumentNames.size) {
            return result.failure(
                RuntimeError(
                    this.start!!,
                    this.end!!,
                    "Too many arguments passed into ${this.identifier} invocation",
                    this.context!!
                )
            )
        } else if (arguments.size < argumentNames.filter { it.defaultValue == null }.size) {
            return result.failure(
                RuntimeError(
                    this.start!!,
                    this.end!!,
                    "Too little arguments passed into ${this.identifier} invocation",
                    this.context!!
                )
            )
        }

        return result.success(null)
    }

    fun populateArguments(argumentNames: List<Argument>, arguments: List<Value>, context: Context) {
        arguments.forEachIndexed { index, value ->
            val arg = argumentNames[index]
            val argValue = arguments[index]
            argValue.setContext(context)
            context.symbolTable!!.set(arg.name, argValue, SymbolTable.EntryData(immutable = true, declaration = true, start = this.start, end = this.end, context = context, forced = true))
        }

        argumentNames.forEach {
            if (!context.symbolTable!!.symbols.any { symbol -> symbol.identifier == it.name } && it.defaultValue != null) {
                context.symbolTable!!.set(it.name, it.defaultValue, SymbolTable.EntryData(immutable = true, declaration = true, start = this.start, end = this.end, context = context, forced = true))
            }
        }
    }

    fun checkAndPopulateArguments(argumentNames: List<Argument>, arguments: List<Value>, context: Context): RuntimeResult {
        val result = RuntimeResult()

        result.register(this.checkArguments(argumentNames, arguments))

        if (result.shouldReturn()) {
            return result
        }

        this.populateArguments(argumentNames, arguments, context)

        return result.success(null)
    }

    data class Argument(val name: String, val defaultValue: Value? = null) {
        override fun toString(): String {
            return "<$name, $defaultValue>"
        }
    }

}