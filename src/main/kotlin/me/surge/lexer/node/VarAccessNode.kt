package me.surge.lexer.node

import me.surge.lexer.token.Token

data class VarAccessNode(val name: Token, var index: Node? = null) : Node(name.start, name.end) {

    override fun toString(): String {
        return "<Variable Access: $name>"
    }

}