package me.surge.util

import me.surge.lexer.position.Position

fun arrows(text: String, start: Position, end: Position): String {
    var result = ""

    var startIndex = Integer.max(text.lastIndexOf('\n', start.index), 0)
    var endIndex = text.indexOf('\n', startIndex + 1)

    if (endIndex < 0) {
        endIndex = text.length
    }

    val lineCount = end.line - start.line + 1

    for (i in 0..lineCount) {
        val line = text.substring(startIndex, endIndex)

        val startColumn = if (i == 0) start.column + 1 else 0
        val endColumn = if (i == lineCount - 1) end.column else line.length - 1

        result += "$line\n"
        result += " ".multiply(startColumn - 1) + "^".multiply(endColumn - startColumn)

        startIndex = endIndex
        endIndex = text.indexOf('\n', startIndex + 1)

        if (endIndex < 0) {
            endIndex = text.length
        }
    }

    return result.replace("\t", "")
}

fun String.multiply(n: Int): String {
    var result = ""

    for (i in 1..n) {
        result += this
    }

    return result
}

fun Boolean.binary(): Int {
    return if (this) 1 else 0
}

fun Int.boolean(): Boolean {
    return this == 1
}

fun List<*>.allIndexed(predicate: (index: Int, element: Any?) -> Boolean): Boolean {
    this.forEachIndexed { index, any ->
        if (!predicate(index, any)) {
            return false
        }
    }

    return true
}

fun List<*>.firstIndexed(predicate: (index: Int, element: Any?) -> Boolean): Any? {
    this.forEachIndexed { index, any ->
        if (predicate(index, any)) {
            return any
        }
    }

    return null
}