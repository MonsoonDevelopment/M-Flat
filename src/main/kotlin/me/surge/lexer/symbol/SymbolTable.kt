package me.surge.lexer.symbol

import me.surge.lexer.error.Error
import me.surge.lexer.error.context.Context
import me.surge.lexer.error.impl.RuntimeError
import me.surge.lexer.position.Position

class SymbolTable(val parent: SymbolTable? = null) {

    val symbols = hashMapOf<String, Pair<Any, Boolean>>()

    fun get(name: String): Any? {
        val value = this.symbols.getOrDefault(name, null)

        if (value == null && this.parent != null) {
            return this.parent.get(name)
        }

        if (value == null) {
            return null
        }

        return value.first
    }

    fun set(name: String, value: Any, final: Boolean = false, start: Position? = null, end: Position? = null, context: Context? = null, declaration: Boolean = false): Error? {
        val variable = this.get(name)

        if (variable != null) {
            if (declaration) {
                return RuntimeError(
                    start!!,
                    end!!,
                    "Attempted to declare variable with existing name",
                    context!!
                )
            }

            if (this.symbols[name]!!.second) {
                return RuntimeError(
                    start!!,
                    end!!,
                    "Attempted to write to final variable",
                    context!!
                )
            }
        }

        this.symbols[name] = Pair(value, final)

        return null
    }

    fun remove(name: String, start: Position? = null, end: Position? = null, context: Context? = null): Error? {
        return if (this.symbols.containsKey(name)) {
            this.symbols.remove(name)
            null
        } else {
            RuntimeError(
                start!!,
                end!!,
                "Attempted to remove '$name', which wasn't defined.",
                context!!
            )
        }
    }

    fun removeGlobally(name: String, start: Position? = null, end: Position? = null, context: Context? = null): Error? {
        if (this.symbols.containsKey(name)) {
            this.symbols.remove(name)
            return null
        } else {
            if (this.parent != null) {
                return this.parent.removeGlobally(name, start, end, context)
            }

            return RuntimeError(
                start!!,
                end!!,
                "Attempted to remove '$name', which wasn't defined.",
                context!!
            )
        }
    }

}