package me.surge.lang.lexer.token

import me.surge.lang.lexer.position.Position

class Token(val type: TokenType, val value: Any? = null, start: Position, end: Position? = null) {

    var start: Position
    var end: Position

    init {
        this.start = start.clone()
        this.end = start.clone()
        this.end.advance()

        if (end != null) {
            this.end = end
        }
    }

    fun matches(type: TokenType, value: Any?): Boolean {
        return this.type == type && this.value == value
    }

    override fun toString(): String {
        return "$type${ if (this.value != null) ":" + this.value else "" }"
    }

}