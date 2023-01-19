package me.surge.lexer.node

import me.surge.lexer.token.Token

data class VarAssignNode(val name: Token, val value: Node, val declaration: Boolean = false, val final: Boolean = false) : Node(name.start, value.end) {

    override fun toString(): String {
        return "<Variable Assignation: $name, $value>"
    }

}