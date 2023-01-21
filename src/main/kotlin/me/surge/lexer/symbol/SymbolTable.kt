package me.surge.lexer.symbol

import me.surge.lexer.error.Error
import me.surge.lexer.error.context.Context
import me.surge.lexer.error.impl.RuntimeError
import me.surge.lexer.position.Position
import me.surge.lexer.value.ContainerValue

class SymbolTable(val name: String, val parent: SymbolTable? = null) {

    val symbols = hashMapOf<String, Pair<Any, Boolean>>()

    fun get(name: String, tableName: String = ""): Any? {
        if (this.name == "global") {
            val value = this.symbols.getOrDefault(tableName, null)

            if (value != null) {
                return (value.first as ContainerValue<SymbolTable>).value.get(name)
            }
        }

        if (this.parent != null && tableName.isNotEmpty() && this.name != tableName) {
            return this.parent.get(name, tableName)
        }

        val value = this.symbols.getOrDefault(name, null)

        if (value == null && this.parent != null) {
            return this.parent.get(name)
        }

        if (value == null) {
            return null
        }

        return value.first
    }

    fun getValueAndFinality(name: String, tableName: String = ""): Pair<Any, Boolean>? {
        if (this.name == "global") {
            val table = this.symbols.getOrDefault(tableName, null)

            if (table != null) {
                return (table.first as ContainerValue<SymbolTable>).value.getValueAndFinality(name, tableName)
            }
        }

        if (this.parent != null && tableName.isNotEmpty() && this.name != tableName) {
            return this.parent.getValueAndFinality(name, tableName)
        }

        val value = this.symbols.getOrDefault(name, null)

        if (value == null && this.parent != null) {
            return this.parent.getValueAndFinality(name)
        }

        if (value == null) {
            return null
        }

        return value
    }

    fun set(name: String, value: Any, final: Boolean = false, start: Position? = null, end: Position? = null, context: Context? = null, declaration: Boolean = false, tableName: String = ""): Error? {
        val variable = this.get(name, tableName)

        if (variable != null) {
            if (declaration) {
                return RuntimeError(
                    start!!,
                    end!!,
                    "Attempted to declare variable with existing name",
                    context!!
                )
            }

            if (this.getValueAndFinality(name, tableName)!!.second) {
                return RuntimeError(
                    start!!,
                    end!!,
                    "Attempted to write to final variable",
                    context!!
                )
            }
        }

        if (this.name == "global") {
            val table = this.symbols.getOrDefault(tableName, null)

            if (table != null) {
                (table.first as ContainerValue<SymbolTable>).value.set(name, value, final, start, end, context, declaration, tableName)
            }
        }

        if (this.parent != null && tableName.isNotEmpty() && this.name != tableName) {
            this.parent.set(name, value, final, start, end, context, declaration, tableName)
        }

        if (!this.symbols.containsKey(name) && this.parent != null) {
            this.parent.set(name, value, final, start, end, context, declaration)
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