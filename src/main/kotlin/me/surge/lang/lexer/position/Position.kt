package me.surge.lang.lexer.position

class Position(var index: Int, var line: Int, var column: Int, var file: String, var fileText: String) {

    fun advance(character: Char? = null): Position {
        this.index++
        this.column++

        if (character != null && character == '\n') {
            this.line++
            this.column = 0
        }

        return this
    }

    fun clone(): Position {
        return Position(this.index, this.line, this.column, this.file, this.fileText)
    }

    override fun toString(): String {
        return "(Pos: ${this.line}, ${this.column})"
    }

}