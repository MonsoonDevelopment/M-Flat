package me.surge.lexer.error.impl

import me.surge.lexer.error.Error
import me.surge.lexer.position.Position

class ExpectedCharError(start: Position, end: Position, details: String) : Error(start, end, "ExpectedCharError", details)