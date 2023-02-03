package me.surge.lexer.node

import me.surge.lexer.position.Position
import me.surge.lexer.token.Token
import me.surge.parse.Parser

class EnumDefinitionNode(val name: Token, val arguments: List<Parser.ArgumentToken>, val members: LinkedHashMap<Token, List<Node>>, val body: Node?, start: Position, end: Position) : Node(start, end) {

    override fun toString(): String {
        return "<Enum Definition: ${this.name}, ${this.arguments}>"
    }

}