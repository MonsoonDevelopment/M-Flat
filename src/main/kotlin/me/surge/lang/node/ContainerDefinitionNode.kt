package me.surge.lang.node

import me.surge.lang.lexer.token.Token
import me.surge.lang.parse.Parser

class ContainerDefinitionNode(val name: Token, val argumentTokens: HashMap<Int, ArrayList<Parser.ArgumentToken>>, endToken: Token, val body: Node?) : Node(name.start, endToken.end) {

    override fun toString(): String {
        return "<Container Define: ${this.name}, ${this.argumentTokens}>"
    }

}