package me.surge.api.result

import me.surge.lexer.value.Value

class Success(value: Value? = null) : Result(value, null)