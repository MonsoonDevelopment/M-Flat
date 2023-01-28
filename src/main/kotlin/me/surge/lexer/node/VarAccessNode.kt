package me.surge.lexer.node

import me.surge.lexer.token.Token
import me.surge.lexer.value.Value

data class VarAccessNode(val name: Token, var args: ArrayList<Value> = arrayListOf()) : Node(name.start, name.end) {

    override fun toString(): String {
        return "<Variable Access: $name>"
    }

}