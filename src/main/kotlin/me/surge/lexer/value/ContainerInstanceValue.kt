package me.surge.lexer.value

import me.surge.lexer.symbol.SymbolTable

@ValueName("instance")
class ContainerInstanceValue(name: String, val value: SymbolTable, val parent: ContainerValue?) : Value(name) {

    override fun clone(): Value {
        return this
    }

    override fun rawValue(): String {
        return "<instance of ${parent?.name}>"
    }

}