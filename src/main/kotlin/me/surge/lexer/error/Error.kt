package me.surge.lexer.error

import me.surge.lexer.position.Position
import me.surge.util.arrows

open class Error(val start: Position, val end: Position, val name: String, val details: String) {

    override fun toString(): String {
        return "$name: $details\nFile ${start.file}, line ${start.line + 1}\n\n${arrows(this.start.fileText, this.start, this.end)}"
    }

}