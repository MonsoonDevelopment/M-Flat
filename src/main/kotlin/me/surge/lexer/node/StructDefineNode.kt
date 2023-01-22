package me.surge.lexer.node

import me.surge.lexer.token.Token

class StructDefineNode(val name: Token, val argumentTokens: ArrayList<Token>, val endToken: Token) : Node(if (name != null) name.start else if (argumentTokens.isNotEmpty()) argumentTokens[0].start else endToken.start, endToken.end) {

    override fun toString(): String {
        return "<Struct Define: ${this.name}, ${this.argumentTokens}>"
    }

}