package me.surge.lexer

import me.surge.Constants.DIGITS
import me.surge.Constants.KEYWORDS
import me.surge.Constants.LETTERS
import me.surge.Constants.LETTERS_DIGITS
import me.surge.lexer.error.Error
import me.surge.lexer.error.impl.ExpectedCharError
import me.surge.lexer.error.impl.IllegalCharError
import me.surge.lexer.position.Position
import me.surge.lexer.token.Token
import me.surge.lexer.token.TokenType.*

class Lexer(val file: String, val text: String) {

    private val position = Position(-1, 0, -1, file, text)
    private var currentChar: Char? = null

    init {
        advance()
    }

    fun advance() {
        this.position.advance(this.currentChar)
        this.currentChar = if (this.position.index < this.text.length) this.text[this.position.index] else null
    }

    fun makeTokens(): Pair<List<Token>, Error?> {
        val tokens = arrayListOf<Token>()

        while (this.currentChar != null) {
            when (this.currentChar!!) {
                in " \t" -> this.advance()

                in ";${System.lineSeparator()}" -> {
                    tokens.add(Token(NEW_LINE, start = this.position))
                    this.advance()
                }

                in DIGITS -> tokens.add(makeNumber())
                in LETTERS -> tokens.add(makeIdentifier())

                '"' -> {
                    tokens.add(this.makeString())
                }

                '+' -> {
                    tokens.add(Token(PLUS, start = this.position))
                    this.advance()
                }

                '-' -> {
                    tokens.add(this.makeMinusOrArrow())
                }

                '*' -> {
                    tokens.add(Token(MULTIPLY, start = this.position))
                    this.advance()
                }

                '/' -> {
                    val token = this.makeCommentOrDivide()

                    if (token != null) {
                        tokens.add(token)
                    }
                }

                '^' -> {
                    tokens.add(Token(POWER, start = this.position))
                    this.advance()
                }

                '%' -> {
                    tokens.add(Token(MODULO, start = this.position))
                    this.advance()
                }

                '(' -> {
                    tokens.add(Token(LEFT_PARENTHESES, start = this.position))
                    this.advance()
                }

                ')' -> {
                    tokens.add(Token(RIGHT_PARENTHESES, start = this.position))
                    this.advance()
                }

                '[' -> {
                    tokens.add(Token(LEFT_SQUARE, start = this.position))
                    this.advance()
                }

                ']' -> {
                    tokens.add(Token(RIGHT_SQUARE, start = this.position))
                    this.advance()
                }

                '!' -> {
                    val result = this.makeNotEquals()

                    if (result.second != null) {
                        return Pair(arrayListOf(), result.second)
                    }

                    tokens.add(result.first!!)
                }

                '=' -> {
                    tokens.add(this.makeEquals())
                }

                '<' -> {
                    tokens.add(this.makeLessThan())
                }

                '>' -> {
                    tokens.add(this.makeGreaterThan())
                }

                ',' -> {
                    tokens.add(Token(COMMA, start = this.position))
                    this.advance()
                }

                ':' -> {
                    val result = this.makeAccessor()

                    if (result.second != null) {
                        return Pair(arrayListOf(), result.second)
                    }

                    tokens.add(result.first!!)
                }

                else -> {
                    val start = this.position.clone()
                    val char = this.currentChar!!

                    this.advance()

                    return Pair(arrayListOf(), IllegalCharError(start, this.position, "'$char'"))
                }
            }
        }

        tokens.add(Token(EOF, start = this.position))

        return Pair(tokens, null)
    }

    private fun makeNumber(): Token {
        var number = ""
        var dots = 0

        while (this.currentChar != null && this.currentChar!! in "$DIGITS.") {
            number += if (this.currentChar == '.') {
                if (dots == 1) {
                    break
                }

                dots++
                '.'
            } else {
                this.currentChar
            }

            this.advance()
        }

        return if (dots == 0) {
            Token(INT, number.toInt(), this.position)
        } else {
            Token(FLOAT, number.toFloat(), this.position)
        }
    }

    private fun makeIdentifier(): Token {
        var id = ""
        val start = this.position.clone()

        while (this.currentChar != null && this.currentChar!! in LETTERS_DIGITS) {
            id += this.currentChar
            this.advance()
        }

        val type = if (KEYWORDS.containsValue(id)) KEYWORD else IDENTIFIER
        return Token(type, id, start, this.position)
    }

    private fun makeNotEquals(): Pair<Token?, Error?> {
        val start = this.position.clone()
        this.advance()

        if (this.currentChar == '=') {
            this.advance()
            return Pair(Token(INEQUALITY, start = start, end = this.position), null)
        }

        this.advance()

        return Pair(null, ExpectedCharError(start, this.position, "'=' after '!'"))
    }

    private fun makeEquals(): Token {
        var type = EQUALS
        val start = this.position.clone()
        this.advance()

        if (this.currentChar == '=') {
            this.advance()
            type = EQUALITY
        }

        return Token(type, start, this.position)
    }

    private fun makeLessThan(): Token {
        var type = LESS_THAN
        val start = this.position.clone()
        this.advance()

        if (this.currentChar == '=') {
            this.advance()
            type = LESS_THAN_OR_EQUAL_TO
        }

        return Token(type, start, this.position)
    }

    private fun makeGreaterThan(): Token {
        var type = GREATER_THAN
        val start = this.position.clone()
        this.advance()

        if (this.currentChar == '=') {
            this.advance()
            type = GREATER_THAN_OR_EQUAL_TO
        }

        return Token(type, start, this.position)
    }

    private fun makeMinusOrArrow(): Token {
        var type = MINUS
        val start = this.position.clone()
        this.advance()

        if (this.currentChar!! == '>') {
            this.advance()
            type = ARROW
        }

        return Token(type, start = start, end = this.position)
    }

    private fun makeString(): Token {
        var result = ""
        val start = this.position.clone()

        var escape = false
        this.advance()

        val escapeCharacters = hashMapOf(
            Pair('n', '\n'),
            Pair('t', '\t')
        )

        while (this.currentChar != null && (this.currentChar != '"' || escape)) {
            if (escape) {
                result += escapeCharacters[this.currentChar]
            } else {
                if (this.currentChar == '\\') {
                    escape = true
                } else {
                    result += this.currentChar
                }
            }

            this.advance()
            escape = false
        }

        this.advance()
        return Token(STRING, result, start, this.position)
    }

    private fun makeCommentOrDivide(): Token? {
        val start = this.position.clone()
        this.advance()

        if (this.currentChar!! == '/') {
            while (this.currentChar != null && this.currentChar != '\n') {
                this.advance()
            }

            return null
        }

        this.advance()

        return Token(DIVIDE, start = start)
    }

    private fun makeAccessor(): Pair<Token?, Error?> {
        val start = this.position.clone()
        this.advance()

        if (this.currentChar == ':') {
            this.advance()
            return Pair(Token(ACCESSOR, start = start, end = this.position), null)
        }

        this.advance()

        return Pair(null, ExpectedCharError(start, this.position, "':' after ':'"))
    }

}