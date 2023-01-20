package me.surge.lexer.error.impl

import me.surge.lexer.error.Error
import me.surge.lexer.error.context.Context
import me.surge.lexer.position.Position
import me.surge.util.arrows

class RuntimeError(start: Position, end: Position, details: String, val context: Context) : Error(start, end, "RuntimeError", details) {

    /* override fun toString(): String {
        return "${this.generateTraceback()}$name: $details\n\n${arrows(this.start.fileText, this.start, this.end)}"
    } */

    fun generateTraceback(): String {
        var result = ""
        var position: Position? = this.start
        var context: Context? = this.context

        while (context != null) {
            result = "  File ${position!!.file}, line ${position.line + 1}, in ${context.displayName}\n$result"
            position = context.parentEntryPosition
            context = context.parent
        }

        return "Traceback (most recent call last):\n$result"
    }

}