package me.surge.lexer.node

import me.surge.lexer.token.Token
import me.surge.parse.Parser

class ContainerDefinitionNode(val name: Token, val argumentTokens: HashMap<Int, ArrayList<Parser.ArgumentToken>>, endToken: Token, val body: Node?) : Node(name.start, endToken.end) {

    override fun toString(): String {
        return "<Container Define: ${this.name}, ${this.argumentTokens}>"
    }

}