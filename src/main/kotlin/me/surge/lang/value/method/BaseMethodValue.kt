package me.surge.lang.value.method

import me.surge.lang.error.Error
import me.surge.lang.error.context.Context
import me.surge.lang.error.impl.RuntimeError
import me.surge.lang.parse.RuntimeResult
import me.surge.lang.symbol.SymbolTable
import me.surge.lang.value.InstanceValue
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

    fun populateArguments(argumentNames: List<Argument>, arguments: List<Value>, context: Context): Error? {
        arguments.forEachIndexed { index, value ->
            val arg = argumentNames[index]
            val argValue = arguments[index]
            argValue.setContext(context)

            if (!argValue.isOfType(arg.type)) {
                return RuntimeError(
                    value.start!!,
                    value.end!!,
                    "${argValue.name} (${argValue.identifier}) is not of type ${arg.type}",
                    context
                )
            }

            val error = context.symbolTable!!.set(arg.name, argValue, SymbolTable.EntryData(immutable = argumentNames[index].final, declaration = true, start = this.start, end = this.end, context = context))

            if (error != null) {
                return error
            }
        }

        argumentNames.forEach {
            if (!context.symbolTable!!.symbols.any { symbol -> symbol.identifier == it.name } && it.defaultValue != null) {
                val error = context.symbolTable!!.set(it.name, it.defaultValue, SymbolTable.EntryData(immutable = it.final, declaration = true, start = this.start, end = this.end, context = context))

                if (error != null) {
                    return error
                }
            }
        }

        return null
    }

    fun checkAndPopulateArguments(argumentNames: List<Argument>, arguments: List<Value>, context: Context): RuntimeResult {
        val result = RuntimeResult()

        result.register(this.checkArguments(argumentNames, arguments))

        if (result.shouldReturn()) {
            return result
        }

        val error = this.populateArguments(argumentNames, arguments, context)

        if (error != null) {
            return result.failure(error)
        }

        return result.success(null)
    }

    data class Argument(val name: String, val defaultValue: Value? = null, val final: Boolean = false, val type: String = "value") {
        override fun toString(): String {
            return "<$name, $defaultValue>"
        }
    }

}