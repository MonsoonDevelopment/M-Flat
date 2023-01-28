package me.surge.lexer.node

class MethodCallNode(val target: Node, val arguments: ArrayList<Node>, child: Node? = null) : Node(target.start, if (arguments.isNotEmpty()) arguments.last().end else target.end, child) {

    override fun toString(): String {
        return "<Method Call: ${this.target}>"
    }

}