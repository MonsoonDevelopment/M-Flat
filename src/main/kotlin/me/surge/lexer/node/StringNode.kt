package me.surge.lexer.node

import me.surge.lexer.token.Token

class StringNode(val token: Token) : Node(token.start!!, token.end!!) {

    override fun toString(): String {
        return "<String: $token>"
    }

}