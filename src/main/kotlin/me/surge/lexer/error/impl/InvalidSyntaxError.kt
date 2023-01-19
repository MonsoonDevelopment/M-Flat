package me.surge.lexer.error.impl

import me.surge.lexer.error.Error
import me.surge.lexer.position.Position

class InvalidSyntaxError(start: Position, end: Position, details: String = "") : Error(start, end, "InvalidSyntaxError", details)