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
        return "<instance $identifier>"
    }

}