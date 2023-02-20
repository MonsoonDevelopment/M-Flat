package me.surge.lang.node

import me.surge.lang.lexer.token.Token
import me.surge.lang.parse.Parser

class MethodDefineNode(val name: Token?, val argumentTokens: ArrayList<Parser.ArgumentToken>, val endToken: Token, val body: Node?, val shouldReturnNull: Boolean) : Node(if (name != null) name.start else if (argumentTokens.isNotEmpty()) argumentTokens[0].token.start else endToken.start, endToken.end) {

    override fun toString(): String {
        return "<Method Define: ${this.name}, ${this.body}>"
    }

}