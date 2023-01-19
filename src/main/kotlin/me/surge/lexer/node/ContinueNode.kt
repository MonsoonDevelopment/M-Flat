package me.surge.lexer.node

import me.surge.lexer.position.Position

class ContinueNode(start: Position, end: Position) : Node(start, end) {

    override fun toString(): String {
        return "<Continue>"
    }

}