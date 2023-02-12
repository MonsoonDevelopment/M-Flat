package me.surge.lang.node

import me.surge.lang.lexer.position.Position

class ContinueNode(start: Position, end: Position) : Node(start, end) {

    override fun toString(): String {
        return "<Continue>"
    }

}