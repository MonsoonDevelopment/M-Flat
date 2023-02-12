package me.surge.lang.node

import me.surge.lang.lexer.token.Token

data class NumberNode(val token: Token) : Node(token.start, token.end) {

    override fun toString(): String {
        return "<Number: $token>"
    }

}