package me.surge.api.result

import me.surge.lexer.error.Error

class Failure(error: Error) : Result(null, error)