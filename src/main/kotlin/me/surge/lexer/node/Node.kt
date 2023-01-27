package me.surge.lexer.node

import me.surge.lexer.position.Position

open class Node(val start: Position, val end: Position, var child: Node? = null)