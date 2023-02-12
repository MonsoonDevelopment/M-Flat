package me.surge.lang.value

import me.surge.lang.error.context.Context
import me.surge.lang.lexer.position.Position
import me.surge.lang.value.link.JvmLinkMethod

data class FunctionData(
    val start: Position?,
    val end: Position?,
    val context: Context?,
    val arguments: List<Value>,
    val instance: JvmLinkMethod
)