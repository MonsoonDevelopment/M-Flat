package me.surge.lang.node

import me.surge.lang.lexer.token.Token
import me.surge.lang.lexer.token.TokenType

data class VarAssignNode(val name: Token, val value: Node, val declaration: Boolean = false, val final: Boolean = false, val mutate: TokenType? = null) : Node(name.start, value.end) {

    override fun toString(): String {
        return "<Variable Assignation: $name, $value>"
    }

}