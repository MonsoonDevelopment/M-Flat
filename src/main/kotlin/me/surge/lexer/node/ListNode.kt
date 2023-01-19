package me.surge.lexer.node

import me.surge.lexer.position.Position

class ListNode(val elements: ArrayList<Node>, start: Position, end: Position) : Node(start, end) {

    override fun toString(): String {
        return "<List: $elements>"
    }

}