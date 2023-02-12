package me.surge.lang.node

import me.surge.lang.lexer.position.Position
import me.surge.lang.lexer.token.Token
import me.surge.lang.parse.Parser

class EnumDefinitionNode(val name: Token, val arguments: List<Parser.ArgumentToken>, val members: LinkedHashMap<Token, List<Node>>, val body: Node?, start: Position, end: Position) : Node(start, end) {

    override fun toString(): String {
        return "<Enum Definition: ${this.name}, ${this.arguments}>"
    }

}