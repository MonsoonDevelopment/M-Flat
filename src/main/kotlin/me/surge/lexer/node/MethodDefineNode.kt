package me.surge.lexer.node

import me.surge.lexer.token.Token

class MethodDefineNode(val name: Token?, val argumentTokens: ArrayList<Token>, val endToken: Token, val body: Node?, val shouldReturnNull: Boolean) : Node(if (name != null) name.start else if (argumentTokens.isNotEmpty()) argumentTokens[0].start else endToken.start, endToken.end) {

    override fun toString(): String {
        return "<Method Define: ${this.name}, ${this.body}>"
    }

}