package me.surge.lang.node

import me.surge.lang.lexer.token.Token

class StringNode(val token: Token, val indexStart: Node?, val indexEnd: Node?) : Node(token.start, token.end) {

    override fun toString(): String {
        return "<String: $token, $indexStart, $indexEnd>"
    }

}