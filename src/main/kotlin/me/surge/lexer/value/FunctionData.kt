package me.surge.lexer.value

import me.surge.lexer.error.context.Context
import me.surge.lexer.position.Position
import me.surge.lexer.value.method.BuiltInMethod

data class FunctionData(
    val start: Position,
    val end: Position,
    val context: Context,
    val arguments: List<Value>,
    val instance: BuiltInMethod
)