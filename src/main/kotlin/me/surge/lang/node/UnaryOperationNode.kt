package me.surge.lang.node

import me.surge.lang.lexer.token.Token

data class UnaryOperationNode(val token: Token, val node: Node) : Node(token.start!!, node.end) {

    override fun toString(): String {
        return "<Unary Operation: ${this.token}, ${this.node}>"
    }

}