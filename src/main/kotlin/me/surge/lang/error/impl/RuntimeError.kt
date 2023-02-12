package me.surge.lang.error.impl

import me.surge.lang.error.Error
import me.surge.lang.error.context.Context
import me.surge.lang.lexer.position.Position

class RuntimeError(start: Position, end: Position, details: String, val context: Context) : Error(start, end, "RuntimeError", details) {

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

    override fun toString(): String {
        return super.toString() + "\n" + generateTraceback()
    }

}