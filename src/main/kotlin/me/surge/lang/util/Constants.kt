package me.surge.lang.util

object Constants {

    const val DIGITS = "0123456789"
    const val LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_"
    const val LETTERS_DIGITS = LETTERS + DIGITS
    const val ALLOWED_SYMBOLS = ":&?{}!>$"

    // holds current flavour

    var KEYWORDS = hashMapOf(
        Pair("var", "mut"),
        Pair("val", "const"),
        Pair("if", "if"),
        Pair("elif", "elseif"),
        Pair("else", "else"),
        Pair("for", "for"),
        Pair("step", "step"),
        Pair("while", "while"),
        Pair("function", "meth"),
        Pair("return", "return"),
        Pair("continue", "continue"),
        Pair("break", "break"),
        Pair("in", "in"),
        Pair("container", "container"),
        Pair("to", "to"),
        Pair("enum", "enum"),
        Pair("and", "&"),
        Pair("or", "?"),
        Pair("not", "!"),
        Pair("then", "{"),
        Pair("end", "}"),
        Pair("accessor", "::"),
        Pair("index splitter", ":"),
        Pair("import", "$")
    )

    fun get(identifier: String): String {
        return KEYWORDS[identifier]!!
    }

}