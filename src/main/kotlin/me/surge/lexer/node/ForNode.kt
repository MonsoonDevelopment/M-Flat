package me.surge.lexer.node

import me.surge.lexer.token.Token

class ForNode(val name: Token, val startValue: Node, val endValue: Node, val step: Node?, val body: Node, val shouldReturnNull: Boolean) : Node(name.start, body.end) {

    override fun toString(): String {
        return "<For: ${this.name}, ${this.step}, ${this.body}>"
    }

}