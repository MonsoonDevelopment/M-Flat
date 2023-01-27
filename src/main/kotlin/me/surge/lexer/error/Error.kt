package me.surge.lexer.error

import me.surge.lexer.position.Position
import me.surge.util.arrows
import kotlin.random.Random
import kotlin.random.nextInt

open class Error(val start: Position, val end: Position, val name: String, val details: String) {

    fun generateMessage(): String {
        val messages = arrayOf(
            "skill issue",
            "(beginning dox due to detected error)",
            "get fucking good",
            "deleting system32",
            "token logging..."
        )

        return messages[(Random.nextFloat() * (messages.size)).toInt()]
    }

    override fun toString(): String {
        return "${generateMessage()}\n\n$name: $details\nFile ${start.file}, line ${start.line + 1}"
    }

}