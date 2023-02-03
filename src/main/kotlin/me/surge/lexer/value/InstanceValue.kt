package me.surge.lexer.value

import me.surge.lexer.symbol.SymbolTable

class InstanceValue(identifier: String, table: SymbolTable) : Value(identifier, "instance") {

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