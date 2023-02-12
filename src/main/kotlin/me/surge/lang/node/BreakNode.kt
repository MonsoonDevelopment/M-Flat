package me.surge.lang.node

import me.surge.lang.lexer.position.Position

class BreakNode(start: Position, end: Position) : Node(start, end) {

    override fun toString(): String {
        return "<Break>"
    }

}