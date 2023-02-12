package me.surge.lang.error.context

import me.surge.lang.lexer.position.Position
import me.surge.lang.symbol.SymbolTable

data class Context(val displayName: String, val parent: Context? = null, val parentEntryPosition: Position? = null) {

    var symbolTable: SymbolTable? = null

    fun createChildSymbolTable(): Context {
        this.symbolTable = SymbolTable(parent!!.symbolTable)

        return this
    }

}