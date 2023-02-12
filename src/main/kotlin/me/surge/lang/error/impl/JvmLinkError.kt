package me.surge.lang.error.impl

import me.surge.lang.error.Error
import me.surge.lang.lexer.position.Position

class JvmLinkError(start: Position, end: Position, details: String) : Error(start, end, "JvmLinkError", details)