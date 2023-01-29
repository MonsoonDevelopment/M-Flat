package me.surge.lexer.value

import me.surge.lexer.symbol.SymbolTable

@ValueName("instance")
class ContainerInstanceValue(name: String, val value: SymbolTable) : Value(name) {

    override fun clone(): Value {
        return ContainerInstanceValue(name, value)
            .setContext(context)
            .setPosition(this.start, this.end)
    }

}