package me.surge.lang.error.impl

import me.surge.lang.error.Error
import me.surge.lang.lexer.position.Position

class InvalidSyntaxError(start: Position, end: Position, details: String = "") : Error(start, end, "InvalidSyntaxError", details)