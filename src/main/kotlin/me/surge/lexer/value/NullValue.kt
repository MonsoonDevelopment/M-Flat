package me.surge.lexer.value

@ValueName("null")
class NullValue : Value("null") {

    override fun toString(): String {
        return "null"
    }

    override fun rawValue(): String {
        return "null"
    }

}