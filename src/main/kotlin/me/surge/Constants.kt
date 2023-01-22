package me.surge

object Constants {

    const val DIGITS = "0123456789"
    const val LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_"
    const val LETTERS_DIGITS = LETTERS + DIGITS
    const val ALLOWED_SYMBOLS = ":&?{}!>"

    private val KEYWORDS = hashMapOf(
        Pair("var", "mut"),
        Pair("val", "const"),
        //Pair("and", "and"),
        //Pair("or", "or"),
        //Pair("not", "not"),
        Pair("if", "if"),
        //Pair("then", "then"),
        Pair("elif", "elseif"),
        Pair("else", "else"),
        Pair("for", "for"),
        //Pair("to", "to"),
        Pair("step", "step"),
        Pair("while", "while"),
        Pair("function", "meth"), // short for method,
        //Pair("end", "end"),
        Pair("return", "return"),
        Pair("continue", "continue"),
        Pair("break", "break"),
        Pair("in", "in"),
        Pair("struct", "struct")
    )

    private val SYMBOLS = hashMapOf(
        Pair("and", "&"),
        Pair("or", "?"),
        Pair("not", "!"),
        Pair("then", "{"),
        Pair("end", "}"),
        Pair("accessor", "::"),
        Pair("to", ">")
    )

    fun get(identifier: String): String {
        return (if (KEYWORDS.containsKey(identifier)) KEYWORDS[identifier] else SYMBOLS[identifier])!!
    }

    fun isInKeywords(identifier: String): Boolean {
        return KEYWORDS.containsValue(identifier)
    }

}