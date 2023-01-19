package me.surge.lexer.node

class WhileNode(val condition: Node, val body: Node, val shouldReturnNull: Boolean) : Node(condition.start, body.end) {

    override fun toString(): String {
        return "<While: ${this.condition}, ${this.body}>"
    }

}