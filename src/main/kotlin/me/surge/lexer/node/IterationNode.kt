package me.surge.lexer.node

import me.surge.lexer.token.Token

class IterationNode(val name: Token, val list: Token, val body: Node, val shouldReturnNull: Boolean) : Node(name.start, body.end) {

    override fun toString(): String {
        return "<Iteration: ${this.name}, ${this.list}, ${this.body}>"
    }

}