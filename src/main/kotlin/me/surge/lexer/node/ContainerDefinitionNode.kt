package me.surge.lexer.node

import me.surge.lexer.token.Token

class ContainerDefinitionNode(val name: Token, val argumentTokens: ArrayList<Token>, endToken: Token, val body: Node?) : Node(name.start, endToken.end) {

    override fun toString(): String {
        return "<Container Define: ${this.name}, ${this.argumentTokens}>"
    }

}