package me.surge.lexer.node

import me.surge.lexer.token.Token

data class NumberNode(val token: Token) : Node(token.start, token.end) {

    override fun toString(): String {
        return "<Number: $token>"
    }

}