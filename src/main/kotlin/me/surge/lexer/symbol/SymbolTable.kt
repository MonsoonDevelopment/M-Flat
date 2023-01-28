package me.surge.lexer.symbol

import me.surge.lexer.error.Error
import me.surge.lexer.error.context.Context
import me.surge.lexer.error.impl.RuntimeError
import me.surge.lexer.position.Position
import me.surge.lexer.value.Value

class SymbolTable(val parent: SymbolTable? = null) {

    val symbols = arrayListOf<Symbol>()

    fun get(identifier: String): Value? {
        if (!symbols.any { it.identifier == identifier } && parent != null) {
            return parent.get(identifier)
        }

        return symbols.firstOrNull { it.identifier == identifier }?.value
    }

    fun getSymbol(identifier: String): Symbol? {
        if (!symbols.any { it.identifier == identifier } && parent != null) {
            return parent.getSymbol(identifier)
        }

        return symbols.firstOrNull { it.identifier == identifier }
    }

    fun set(identifier: String, value: Value, entry: EntryData): Error? {
        val existingValue = getSymbol(identifier)

        // value already exists
        if (existingValue != null && entry.declaration && !entry.forced) {
            return RuntimeError(
                entry.start!!,
                entry.end!!,
                "'$identifier' was already defined!",
                entry.context!!
            )
        }

        if (existingValue != null && !this.symbols.any { it.identifier == identifier } && parent != null) {
            return this.parent.set(identifier, value, entry)
        }

        if (existingValue == null && entry.declaration) {
            this.symbols.add(Symbol(identifier, value, entry.immutable))
        } else if (existingValue != null && !entry.declaration || entry.forced) {
            this.symbols.first { it.identifier == identifier }.value = value
        } else {
            return RuntimeError(
                entry.start!!,
                entry.end!!,
                "'$identifier' is not defined!",
                entry.context!!
            )
        }

        return null
    }

    fun remove(identifier: String, start: Position? = null, end: Position? = null, context: Context? = null): Error? {
        return if (this.symbols.any { it.identifier == identifier }) {
            this.symbols.removeIf { it.identifier == identifier }
            null
        } else {
            RuntimeError(
                start!!,
                end!!,
                "Attempted to remove '$identifier', which wasn't defined.",
                context!!
            )
        }
    }

    fun removeGlobally(identifier: String, start: Position? = null, end: Position? = null, context: Context? = null): Error? {
        if (this.symbols.any { it.identifier == identifier }) {
            this.symbols.removeIf { it.identifier == identifier }
            return null
        } else {
            if (this.parent != null) {
                return this.parent.removeGlobally(identifier, start, end, context)
            }

            return RuntimeError(
                start!!,
                end!!,
                "Attempted to remove '$identifier', which wasn't defined.",
                context!!
            )
        }
    }

    fun getAll(): ArrayList<Symbol> {
        val list = arrayListOf<Symbol>()

        list.addAll(this.symbols)

        if (this.parent != null) {
            list.addAll(this.parent.getAll())
        }

        return list
    }

    override fun equals(other: Any?): Boolean {
        if (other !is SymbolTable) {
            return false
        }

        this.symbols.forEachIndexed { index, symbol ->
            if (other.symbols[index] != symbol) {
                return false
            }
        }

        return true
    }

    data class Symbol(val identifier: String, var value: Value, val immutable: Boolean)
    data class EntryData(val immutable: Boolean, val declaration: Boolean, val start: Position?, val end: Position?, val context: Context?, val forced: Boolean = false)

}