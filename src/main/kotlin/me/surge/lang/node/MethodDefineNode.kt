package me.surge.lang.node

import me.surge.lang.lexer.token.Token
import me.surge.lang.parse.Parser

class MethodDefineNode(
    val name: Token?,
    val argumentTokens: ArrayList<Parser.ArgumentToken>,
    endToken: Token,
    val body: Node?,
    val returnType: Token?
) : Node(
    name?.start ?: if (argumentTokens.isNotEmpty()) {
        argumentTokens[0].token.start
    } else {
        endToken.start
    },
    endToken.end
) {

    override fun toString(): String {
        return "<Method Define: ${this.name}, ${this.body}>"
    }

}