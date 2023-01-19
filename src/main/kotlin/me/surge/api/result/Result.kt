package me.surge.api.result

import me.surge.lexer.error.Error
import me.surge.lexer.value.Value

open class Result(val value: Value?, val error: Error?)