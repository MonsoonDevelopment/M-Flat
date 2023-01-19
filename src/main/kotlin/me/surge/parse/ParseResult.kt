package me.surge.parse

import me.surge.lexer.error.Error

class ParseResult {

    var error: Error? = null
    var node: Any? = null
    var advancement = 0
    var lastRegisteredAdvanceCount = 0
    var reverseCount = 0

    fun register(result: ParseResult): Any? {
        this.lastRegisteredAdvanceCount = result.advancement
        this.advancement += result.advancement

        if (result.error != null) {
            this.error = result.error
        }

        return result.node
    }

    fun registerAdvancement() {
        this.advancement++
        this.lastRegisteredAdvanceCount = 1
    }

    fun tryRegister(result: ParseResult): Any? {
        if (result.error != null) {
            this.reverseCount = result.reverseCount
            return null
        }

        return this.register(result)
    }

    fun success(node: Any): ParseResult {
        this.node = node
        return this
    }

    fun failure(error: Error): ParseResult {
        if (this.error == null || this.lastRegisteredAdvanceCount == 0) {
            this.error = error
        }

        return this
    }

}