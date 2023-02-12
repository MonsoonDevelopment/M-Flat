package me.surge.lang.node

import me.surge.lang.lexer.token.Token

data class VarAccessNode(val name: Token, var index: Node? = null) : Node(name.start, name.end) {

    override fun toString(): String {
        return "<Variable Access: $name>"
    }

}