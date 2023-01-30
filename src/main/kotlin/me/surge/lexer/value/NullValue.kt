package me.surge.lexer.value

import me.surge.lexer.error.Error

@ValueName("null")
class NullValue : Value("null") {

    override fun compareEquality(other: Value): Pair<BooleanValue?, Error?> {
        return Pair(BooleanValue(other.name, other is NullValue), null)
    }

    override fun compareInequality(other: Value): Pair<BooleanValue?, Error?> {
        return Pair(BooleanValue(other.name, other !is NullValue), null)
    }

    override fun toString(): String {
        return "null"
    }

    override fun rawValue(): String {
        return "null"
    }

}