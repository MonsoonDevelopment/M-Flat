package me.surge.lang.value

import me.surge.lang.symbol.SymbolTable

class InstanceValue(identifier: String, table: SymbolTable, val parent: Value?) : Value(identifier, "instance") {

    init {
        this.setSymbolTable(table)
    }

    override fun toString(): String {
        return stringValue()
    }

    override fun stringValue(): String {
        return super.overriddenString() ?: this.parent?.overriddenString() ?: "<instance $identifier>"
    }

    override fun isOfType(type: String): Boolean {
        if (parent != null) {
            if (type == this.parent.identifier) {
                return true
            }
        }

        return super.isOfType(type)
    }

    override fun type(): String = this.parent?.identifier ?: this.identifier

}