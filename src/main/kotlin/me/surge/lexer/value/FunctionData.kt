package me.surge.lexer.value

import me.surge.lexer.error.context.Context
import me.surge.lexer.position.Position
import me.surge.lexer.value.function.BuiltInFunction

data class FunctionData(
    val start: Position,
    val end: Position,
    val context: Context,
    val arguments: ArrayList<Value>,
    val instance: BuiltInFunction
)
