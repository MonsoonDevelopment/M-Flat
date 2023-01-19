package me.surge.lexer.node

import me.surge.lexer.position.Position

class ReturnNode(val toReturn: Node?, start: Position, end: Position) : Node(start, end) {

    override fun toString(): String {
        return "<Return: $toReturn>"
    }

}