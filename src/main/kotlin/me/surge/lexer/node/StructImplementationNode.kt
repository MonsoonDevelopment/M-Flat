package me.surge.lexer.node

import me.surge.lexer.token.Token

class StructImplementationNode(val name: Token, val body: Node, val endToken: Token) : Node(name.start, endToken.end) {

    override fun toString(): String {
        return "<Struct Implementation: ${this.name}>"
    }

}