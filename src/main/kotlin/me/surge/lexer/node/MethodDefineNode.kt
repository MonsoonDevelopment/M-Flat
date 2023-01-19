package me.surge.lexer.node

import me.surge.lexer.token.Token

class MethodDefineNode(val name: Token?, val argumentTokens: ArrayList<Token>, val body: Node, val shouldReturnNull: Boolean) : Node(if (name != null) name.start else if (argumentTokens.isNotEmpty()) argumentTokens[0].start else body.start, body.end) {

    override fun toString(): String {
        return "<Method Define: ${this.name}, ${this.body}>"
    }

}