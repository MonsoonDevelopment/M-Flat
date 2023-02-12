package me.surge.lang.node

import me.surge.lang.lexer.token.Token

class IterationNode(val name: Token, val list: Token, val body: Node, val shouldReturnNull: Boolean) : Node(name.start, body.end) {

    override fun toString(): String {
        return "<Iteration: ${this.name}, ${this.list}, ${this.body}>"
    }

}