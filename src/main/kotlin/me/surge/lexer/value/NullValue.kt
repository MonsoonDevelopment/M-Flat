package me.surge.lexer.value

import me.surge.lexer.error.Error

class NullValue : Value("null", "null") {

    override fun compareEquality(other: Value): Pair<BooleanValue?, Error?> {
        return Pair(BooleanValue(other.name, other is NullValue), null)
    }

    override fun compareInequality(other: Value): Pair<BooleanValue?, Error?> {
        return Pair(BooleanValue(other.name, other !is NullValue), null)
    }

    override fun toString(): String {
        return stringValue()
    }

    override fun stringValue(): String {
        return "null"
    }

}