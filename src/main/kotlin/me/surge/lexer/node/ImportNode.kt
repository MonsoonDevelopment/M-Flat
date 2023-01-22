package me.surge.lexer.node

import me.surge.lexer.position.Position
import me.surge.lexer.token.Token

class ImportNode(val name: Token, start: Position, end: Position) : Node(start, end) {

    override fun toString(): String {
        return "<Import: $name>"
    }

}