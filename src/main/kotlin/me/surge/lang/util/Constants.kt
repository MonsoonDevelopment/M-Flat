package me.surge.lang.util

object Constants {

    const val DIGITS = "0123456789"
    const val LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_"
    const val LETTERS_DIGITS = LETTERS + DIGITS
    const val ALLOWED_SYMBOLS = ":&?{}!>"

    private val KEYWORDS = hashMapOf(
        Pair("var", "mut"),
        Pair("val", "const"),
        Pair("if", "if"),
        Pair("elif", "elseif"),
        Pair("else", "else"),
        Pair("for", "for"),
        Pair("step", "step"),
        Pair("while", "while"),
        Pair("function", "meth"), // short for method,
        Pair("return", "return"),
        Pair("continue", "continue"),
        Pair("break", "break"),
        Pair("in", "in"),
        Pair("use", "use"),
        Pair("container", "container"),
        Pair("to", "to"),
        Pair("enum", "enum")
    )

    private val SYMBOLS = hashMapOf(
        Pair("and", "&"),
        Pair("or", "?"),
        Pair("not", "!"),
        Pair("then", "{"),
        Pair("end", "}"),
        Pair("accessor", "::"),
        Pair("index splitter", ":")
    )

    fun get(identifier: String): String {
        return (if (KEYWORDS.containsKey(identifier)) KEYWORDS[identifier] else SYMBOLS[identifier])!!
    }

    fun isInKeywords(identifier: String): Boolean {
        return KEYWORDS.containsValue(identifier)
    }

}