package me.surge.lang.node

import me.surge.lang.lexer.position.Position
import me.surge.lang.lexer.token.Token

class ImportNode(val name: Token, val identifier: Token, start: Position, end: Position) : Node(start, end) {

    override fun toString(): String {
        return "<Import: $name>"
    }

}