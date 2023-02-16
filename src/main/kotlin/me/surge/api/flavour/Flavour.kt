package me.surge.api.flavour

/**
 * @author surge
 * @since 16/02/2023
 */
open class Flavour {

    open val ALLOWED_SYMBOLS = ":&?{}!>$"

    val getters = hashMapOf(
        Pair("var") { mutable() },
        Pair("val") { immutable() },
        Pair("if") { `if`() },
        Pair("elif") { elif() },
        Pair("else") { `else`() },
        Pair("for") { `for`() },
        Pair("step") { step() },
        Pair("while") { `while`() },
        Pair("function") { method() },
        Pair("return") { `return`() },
        Pair("continue") { `continue`() },
        Pair("break") { `break`() },
        Pair("in") { `in`() },
        Pair("container") { container() },
        Pair("to") { to() },
        Pair("enum") { enum() },
        Pair("and") { and() },
        Pair("or") { or() },
        Pair("not") { not() },
        Pair("then") { then() },
        Pair("end") { end() },
        Pair("accessor") { accessor() },
        Pair("index splitter") { index() },
        Pair("import") { import() },
    )

    val TOKENS = hashMapOf<String, String>()

    init {
        getters.forEach { name, supplier ->
            TOKENS[name] = supplier()
        }
    }

    open fun mutable(): String = "mut"
    open fun immutable(): String = "const"
    open fun `if`(): String = "if"
    open fun elif(): String = "elseif"
    open fun `else`(): String = "else"
    open fun `for`(): String = "for"
    open fun step(): String = "step"
    open fun `while`(): String = "while"
    open fun method(): String = "meth"
    open fun `return`(): String = "return"
    open fun `continue`(): String = "continue"
    open fun `break`(): String = "break"
    open fun `in`(): String = "in"
    open fun container(): String = "container"
    open fun to(): String = "to"
    open fun enum(): String = "enum"

    open fun and(): String = "&"
    open fun or(): String = "?"
    open fun not(): String = "!"
    open fun then(): String = "{"
    open fun end(): String = "}"
    open fun accessor(): String = "::"
    open fun index(): String = ":"
    open fun import(): String = "$"
    open fun `as`(): String = "->"

    fun get(identifier: String): String {
        return TOKENS[identifier]!!
    }

    fun isInKeywords(identifier: String): Boolean {
        return TOKENS.containsValue(identifier)
    }

    fun matchesSpaces(input: String): Boolean {
        return TOKENS.filter { it.value.contains(' ') }.any { it.value.startsWith(input) }
    }

}