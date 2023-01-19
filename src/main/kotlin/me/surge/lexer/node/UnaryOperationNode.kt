package me.surge.lexer.node

import me.surge.lexer.token.Token

data class UnaryOperationNode(val token: Token, val node: Node) : Node(token.start!!, node.end) {

    override fun toString(): String {
        return "<Unary Operation: ${this.token}, ${this.node}>"
    }

}