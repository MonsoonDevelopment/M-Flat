package me.surge.lang.node

import me.surge.lang.lexer.position.Position

open class Node(val start: Position, val end: Position, var child: Node? = null) {
    var parent: Node? = null
}