package me.surge.lexer.error.context

import me.surge.lexer.position.Position
import me.surge.lexer.symbol.SymbolTable

data class Context(val displayName: String, val parent: Context? = null, val parentEntryPosition: Position? = null) {

    var symbolTable: SymbolTable? = null

    fun createChildSymbolTable(): Context {
        this.symbolTable = SymbolTable(parent!!.symbolTable)

        return this
    }

}