package me.surge.lexer.node

import me.surge.lexer.token.Token

data class BinaryOperationNode(val left: Node, val token: Token, val right: Node) : Node(left.start, right.end) {

    override fun toString(): String {
        return "<Binary Operation: ${this.left}, ${this.token}, ${this.right}>"
    }

}