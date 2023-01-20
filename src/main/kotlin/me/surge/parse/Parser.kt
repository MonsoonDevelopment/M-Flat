package me.surge.parse

import me.surge.Constants
import me.surge.lexer.error.impl.InvalidSyntaxError
import me.surge.lexer.node.*
import me.surge.lexer.token.Token
import me.surge.lexer.token.TokenType
import java.util.function.Supplier

class Parser(val tokens: List<Token>) {

    private var tokenIndex = -1
    private lateinit var currentToken: Token

    init {
        this.advance()
    }

    private fun advance(): Token {
        this.tokenIndex++
        this.updateCurrent()
        return this.currentToken
    }

    private fun reverse(amount: Int = 1): Token {
        this.tokenIndex -= amount
        this.updateCurrent()
        return this.currentToken
    }

    private fun updateCurrent() {
        if (this.tokenIndex >= 0 && this.tokenIndex < tokens.size) {
            this.currentToken = this.tokens[this.tokenIndex]
        }
    }

    fun parse(): ParseResult {
        val result = this.statements()

        if (result.error != null && this.currentToken.type != TokenType.EOF) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '+', '-', '*', '/', '^', '==', '!=', '<', '>', '<=', '>=', '${Constants.KEYWORDS["and"]}' or '${Constants.KEYWORDS["or"]}'"
            ))
        }

        return result
    }

    private fun statements(): ParseResult {
        val result = ParseResult()
        val statements = arrayListOf<Node>()
        val start = this.currentToken.start.clone()

        while (this.currentToken.type == TokenType.NEW_LINE) {
            result.registerAdvancement()
            this.advance()
        }

        val statement = result.register(this.statement())

        if (result.error != null) {
            return result
        }

        statements.add(statement as Node)

        var moreStatements = true

        while (true) {
            var newLineCount = 0

            while (this.currentToken.type == TokenType.NEW_LINE) {
                result.registerAdvancement()
                this.advance()
                newLineCount++
            }

            if (newLineCount == 0) {
                moreStatements = false
            }

            if (!moreStatements) {
                break
            }

            val statement = result.tryRegister(this.expression())

            if (statement == null) {
                this.reverse(result.reverseCount)
                moreStatements = false
                continue
            }

            statements.add(statement as Node)
        }

        return result.success(ListNode(
            statements,
            start,
            this.currentToken.end.clone()
        ))
    }

    private fun statement(): ParseResult {
        val result = ParseResult()
        val start = this.currentToken.start.clone()

        if (this.currentToken.matches(TokenType.KEYWORD, Constants.KEYWORDS["return"])) {
            result.registerAdvancement()
            this.advance()

            val expression = result.tryRegister(this.expression())

            if (expression == null) {
                this.reverse(result.reverseCount)
            }

            return result.success(ReturnNode(expression as Node, start, this.currentToken.end.clone()))
        }

        if (this.currentToken.matches(TokenType.KEYWORD, Constants.KEYWORDS["continue"])) {
            result.registerAdvancement()
            this.advance()
            return result.success(ContinueNode(start, this.currentToken.end.clone()))
        }

        if (this.currentToken.matches(TokenType.KEYWORD, Constants.KEYWORDS["break"])) {
            result.registerAdvancement()
            this.advance()
            return result.success(BreakNode(start, this.currentToken.end.clone()))
        }

        val expression = result.register(this.expression())

        if (result.error != null) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '${Constants.KEYWORDS["return"]}', '${Constants.KEYWORDS["continue"]}', '${Constants.KEYWORDS["break"]}', '${Constants.KEYWORDS["var"]}', '${Constants.KEYWORDS["if"]}', '${Constants.KEYWORDS["for"]}', '${Constants.KEYWORDS["while"]}', '${Constants.KEYWORDS["function"]}', int, float, identifier, '+', '-', '(', '[' or '${Constants.KEYWORDS["not"]}'"
            ))
        }

        return result.success(expression as Node)
    }

    private fun expression(): ParseResult {
        val result = ParseResult()

        if (this.currentToken.matches(TokenType.KEYWORD, Constants.KEYWORDS["var"]) || this.currentToken.matches(TokenType.KEYWORD, Constants.KEYWORDS["val"])) {
            val final = this.currentToken.matches(TokenType.KEYWORD, Constants.KEYWORDS["val"])

            result.registerAdvancement()
            this.advance()

            if (this.currentToken.type != TokenType.IDENTIFIER) {
                return result.failure(InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected identifier"
                ))
            }

            val name = this.currentToken

            result.registerAdvancement()
            this.advance()

            if (this.currentToken.type != TokenType.EQUALS) {
                return result.failure(InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '='"
                ))
            }

            result.registerAdvancement()
            this.advance()

            val expression = result.register(this.expression())

            if (result.error != null) {
                return result
            }

            return result.success(VarAssignNode(name, expression!! as Node, declaration = true, final = final))
        }

        val node = result.register(this.binaryOperation({this.comparisonExpression()}, arrayOf(Pair(TokenType.KEYWORD, Constants.KEYWORDS["and"]), Pair(TokenType.KEYWORD, Constants.KEYWORDS["or"]))))

        if (result.error != null) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '${Constants.KEYWORDS["var"]}', '${Constants.KEYWORDS["if"]}', '${Constants.KEYWORDS["for"]}', '${Constants.KEYWORDS["while"]}', int, float, identifier, '+', '-', '(' or '${Constants.KEYWORDS["not"]}'"
            ))
        }

        return result.success(node as Node)
    }

    private fun comparisonExpression(): ParseResult {
        val result = ParseResult()

        if (this.currentToken.matches(TokenType.KEYWORD, Constants.KEYWORDS["not"])) {
            val operator = this.currentToken
            result.registerAdvancement()
            this.advance()

            val node = result.register(this.comparisonExpression()) as Node

            if (result.error != null) {
                return result
            }

            return result.success(UnaryOperationNode(operator, node))
        }

        val node = result.register(this.binaryOperation({this.arithmeticExpression()}, arrayOf(TokenType.EQUALITY, TokenType.INEQUALITY, TokenType.LESS_THAN, TokenType.GREATER_THAN, TokenType.LESS_THAN_OR_EQUAL_TO, TokenType.GREATER_THAN_OR_EQUAL_TO)))

        if (result.error != null) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected int, float, identifier, '+', '-', '(', '${Constants.KEYWORDS["if"]}', '${Constants.KEYWORDS["for"]}', '${Constants.KEYWORDS["while"]}', '${Constants.KEYWORDS["function"]}' or '${Constants.KEYWORDS["not"]}'"
            ))
        }

        return result.success(node as Node)
    }

    private fun arithmeticExpression(): ParseResult {
        return this.binaryOperation({term()}, arrayOf(TokenType.PLUS, TokenType.MINUS))
    }

    private fun term(): ParseResult {
        return this.binaryOperation({factor()}, arrayOf(TokenType.MULTIPLY, TokenType.DIVIDE))
    }

    private fun factor(): ParseResult {
        val result = ParseResult()
        val token = this.currentToken

        if (token.type in arrayOf(TokenType.PLUS, TokenType.MINUS)) {
            result.registerAdvancement()
            this.advance()
            val factor = result.register(this.factor())

            if (result.error != null) {
                return result
            }

            return result.success(UnaryOperationNode(token, factor as Node))
        }

        return this.power()
    }

    private fun power(): ParseResult {
        return this.binaryOperation({ this.call() }, arrayOf(TokenType.POWER, TokenType.MODULO), { this.factor() })
    }

    private fun call(): ParseResult {
        val result = ParseResult()
        val atom = result.register(this.atom())

        if (result.error != null) {
            return result
        }

        if (this.currentToken.type == TokenType.LEFT_PARENTHESES) {
            result.registerAdvancement()
            this.advance()

            val argumentNodes = arrayListOf<Node>()

            if (this.currentToken.type == TokenType.RIGHT_PARENTHESES) {
                result.registerAdvancement()
                this.advance()
            } else {
                val node = result.register(this.expression())

                if (result.error != null) {
                    return result.failure(InvalidSyntaxError(
                        this.currentToken.start,
                        this.currentToken.end,
                        "Expected ')', '${Constants.KEYWORDS["var"]}', '${Constants.KEYWORDS["if"]}', '${Constants.KEYWORDS["for"]}', '${Constants.KEYWORDS["while"]}', '${Constants.KEYWORDS["function"]}', int, float, identifier, '+', '-', '(', or ''${Constants.KEYWORDS["not"]}'"
                    ))
                }

                argumentNodes.add(node as Node)

                while (this.currentToken.type == TokenType.COMMA) {
                    result.registerAdvancement()
                    this.advance()

                    val node = result.register(this.expression())

                    if (result.error != null) {
                        return result
                    }

                    argumentNodes.add(node as Node)
                }

                if (this.currentToken.type != TokenType.RIGHT_PARENTHESES) {
                    return result.failure(InvalidSyntaxError(
                        this.currentToken.start,
                        this.currentToken.end,
                        "Expected ',' or ')'"
                    ))
                }

                result.registerAdvancement()
                this.advance()
            }

            return result.success(MethodCallNode(atom as Node, argumentNodes))
        }

        return result.success(atom as Node)
    }

    private fun atom(): ParseResult {
        val result = ParseResult()
        val token = this.currentToken

        if (token.type in arrayOf(TokenType.INT, TokenType.FLOAT)) {
            result.registerAdvancement()
            this.advance()
            return result.success(NumberNode(token))
        }

        else if (token.type == TokenType.STRING) {
            result.registerAdvancement()
            this.advance()
            return result.success(StringNode(token))
        }

        else if (token.type == TokenType.IDENTIFIER) {
            var identifierToken: Token = token
            var valueToken: Token = token

            var accessed = false

            result.registerAdvancement()
            this.advance()

            if (this.currentToken.type == TokenType.EQUALS) {
                result.registerAdvancement()
                this.advance()

                val expression = result.register(this.expression())

                if (result.error != null) {
                    return result
                }

                return result.success(VarAssignNode(token, expression!! as Node, declaration = false, final = false))
            } else if (this.currentToken.type == TokenType.ACCESSOR) {
                accessed = true

                result.registerAdvancement()
                this.advance()

                valueToken = this.currentToken

                result.registerAdvancement()
                this.advance()
            }

            return result.success(VarAccessNode(if (accessed) valueToken else identifierToken, if (accessed) identifierToken else null))
        }

        else if (token.type == TokenType.LEFT_PARENTHESES) {
            result.registerAdvancement()
            this.advance()
            val expression = result.register(this.expression())

            if (result.error != null) {
                return result
            }

            return if (this.currentToken.type == TokenType.RIGHT_PARENTHESES) {
                result.registerAdvancement()
                this.advance()
                result.success(expression!! as Node)
            } else {
                result.failure(InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected ')'"
                ))
            }
        }

        else if (token.type == TokenType.LEFT_SQUARE) {
            val listExpression = result.register(this.listExpression())

            if (result.error != null) {
                return result
            }

            return result.success(listExpression as Node)
        }

        else if (token.matches(TokenType.KEYWORD, Constants.KEYWORDS["if"])) {
            val ifExpression = result.register(this.ifExpression())

            if (result.error != null) {
                return result
            }

            return result.success(ifExpression as Node)
        }

        else if (token.matches(TokenType.KEYWORD, Constants.KEYWORDS["for"])) {
            val forExpression = result.register(this.forExpression())

            if (result.error != null) {
                return result
            }

            return result.success(forExpression as Node)
        }

        else if (token.matches(TokenType.KEYWORD, Constants.KEYWORDS["while"])) {
            val whileExpression = result.register(this.whileExpression())

            if (result.error != null) {
                return result
            }

            return result.success(whileExpression as Node)
        }

        else if (token.matches(TokenType.KEYWORD, Constants.KEYWORDS["function"])) {
            val definition = result.register(this.functionDefinition())

            if (result.error != null) {
                return result
            }

            return result.success(definition as Node)
        }

        return result.failure(InvalidSyntaxError(
            token.start,
            token.end,
            "Expected int, float, '+', '-' or '('"
        ))
    }

    private fun listExpression(): ParseResult {
        val result = ParseResult()
        val elementNodes = arrayListOf<Node>()
        val start = this.currentToken.start.clone()

        if (this.currentToken.type != TokenType.LEFT_SQUARE) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '['"
            ))
        }

        result.registerAdvancement()
        this.advance()

        if (this.currentToken.type == TokenType.RIGHT_SQUARE) {
            result.registerAdvancement()
            this.advance()
        } else {
            val node = result.register(this.expression())

            if (result.error != null) {
                return result.failure(InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected ']', '${Constants.KEYWORDS["var"]}', '${Constants.KEYWORDS["if"]}', '${Constants.KEYWORDS["for"]}', '${Constants.KEYWORDS["while"]}', ${Constants.KEYWORDS["fun"]}, int, float, identifier, '+', '-', '*', '/', '(', '[' or '${Constants.KEYWORDS["not"]}'"
                ))
            }

            elementNodes.add(node as Node)

            while (this.currentToken.type == TokenType.COMMA) {
                result.registerAdvancement()
                this.advance()

                val node = result.register(this.expression())

                if (result.error != null) {
                    return result
                }

                elementNodes.add(node as Node)
            }

            if (this.currentToken.type != TokenType.RIGHT_SQUARE) {
                return result.failure(InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected ',' or ']'"
                ))
            }

            result.registerAdvancement()
            this.advance()
        }

        return result.success(ListNode(
            elementNodes,
            start,
            this.currentToken.end.clone()
        ))
    }

    private fun ifExpression(): ParseResult {
        val result = ParseResult()
        val allCases = result.register(this.ifExpressionCases(Constants.KEYWORDS["if"]!!))

        if (result.error != null) {
            return result
        }

        allCases as Pair<ArrayList<Case>, BaseCase?>

        val cases = allCases.first
        val elseCase = allCases.second

        return result.success(IfNode(cases, elseCase))
    }

    private fun ifExpressionB(): ParseResult {
        return this.ifExpressionCases(Constants.KEYWORDS["elif"]!!)
    }

    private fun ifExpressionC(): ParseResult {
        val result = ParseResult()
        var elseCase: BaseCase? = null

        if (this.currentToken.matches(TokenType.KEYWORD, Constants.KEYWORDS["else"])) {
            result.registerAdvancement()
            this.advance()

            if (this.currentToken.type == TokenType.NEW_LINE) {
                result.registerAdvancement()
                this.advance()

                val statements = result.register(this.statements())

                if (result.error != null) {
                    return result
                }

                elseCase = BaseCase(statements as Node, true)

                if (this.currentToken.matches(TokenType.KEYWORD, Constants.KEYWORDS["end"])) {
                    result.registerAdvancement()
                    this.advance()
                } else {
                    return result.failure(InvalidSyntaxError(
                        this.currentToken.start,
                        this.currentToken.end,
                        "Expected '${Constants.KEYWORDS["end"]}'"
                    ))
                }
            } else {
                val expression = result.register(this.statement())

                if (result.error != null) {
                    return result
                }

                elseCase = BaseCase(expression as Node, false)
            }
        }

        return result.success(elseCase!!)
    }

    private fun ifExpressionBorC(): ParseResult {
        val result = ParseResult()
        var cases = arrayListOf<Case>()
        val elseCase: BaseCase?

        if (this.currentToken.matches(TokenType.KEYWORD, Constants.KEYWORDS["elif"])) {
            val allCases = result.register(this.ifExpressionB())

            if (result.error != null) {
                return result
            }

            allCases as Pair<ArrayList<Case>, BaseCase?>

            cases = allCases.first
            elseCase = allCases.second
        } else {
            val case = result.register(this.ifExpressionC())

            if (result.error != null) {
                return result
            }

            elseCase = case as BaseCase
        }

        return result.success(Pair(cases, elseCase))
    }

    private fun ifExpressionCases(keyword: String): ParseResult {
        val result = ParseResult()
        val cases = ArrayList<Case>()
        var elseCase: BaseCase? = null

        if (!this.currentToken.matches(TokenType.KEYWORD, keyword)) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '$keyword'"
            ))
        }

        result.registerAdvancement()
        this.advance()

        val condition = result.register(this.expression())

        if (result.error != null) {
            return result
        }

        condition as Node

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.KEYWORDS["then"])) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected ${Constants.KEYWORDS["then"]}"
            ))
        }

        result.registerAdvancement()
        this.advance()

        if (this.currentToken.type == TokenType.NEW_LINE) {
            result.registerAdvancement()
            this.advance()

            val statements = result.register(this.statements())

            if (result.error != null) {
                return result
            }

            statements as Node

            cases.add(Case(condition, statements, true))

            if (this.currentToken.matches(TokenType.KEYWORD, Constants.KEYWORDS["end"])) {
                result.registerAdvancement()
                this.advance()
            } else {
                val allCases = result.register(this.ifExpressionBorC())

                if (result.error != null) {
                    return result
                }

                allCases as Pair<ArrayList<Case>, BaseCase?>

                val new = allCases.first
                elseCase = allCases.second

                cases.addAll(new)
            }
        } else {
            val expression = result.register(this.statement())

            if (result.error != null) {
                return result
            }

            expression as Node

            cases.add(Case(condition, expression, false))

            val allCases = result.register(this.ifExpressionBorC())

            if (result.error != null) {
                return result
            }

            allCases as Pair<ArrayList<Case>, Case?>

            val newCases = allCases.first
            elseCase = allCases.second

            cases.addAll(newCases)
        }

        return result.success(Pair(cases, elseCase))
    }

    private fun forExpression(): ParseResult {
        val result = ParseResult()

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.KEYWORDS["for"])) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '${Constants.KEYWORDS["for"]}'"
            ))
        }

        result.registerAdvancement()
        this.advance()

        if (this.currentToken.type != TokenType.IDENTIFIER) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected identifier"
            ))
        }

        val name = this.currentToken
        result.registerAdvancement()
        this.advance()

        if (this.currentToken.type != TokenType.EQUALS) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '='"
            ))
        }

        result.registerAdvancement()
        this.advance()

        val start = result.register(this.expression())

        if (result.error != null) {
            return result
        }

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.KEYWORDS["to"])) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '${Constants.KEYWORDS["to"]}'"
            ))
        }

        result.registerAdvancement()
        this.advance()

        val end = result.register(this.expression())

        if (result.error != null) {
            return result
        }

        var step: Node? = null

        if (this.currentToken.matches(TokenType.KEYWORD, Constants.KEYWORDS["step"])) {
            result.registerAdvancement()
            this.advance()

            val s = result.register(this.expression())

            if (result.error != null) {
                return result
            }

            step = s as Node
        }

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.KEYWORDS["then"])) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '${Constants.KEYWORDS["then"]}'"
            ))
        }

        result.registerAdvancement()
        this.advance()

        if (this.currentToken.type == TokenType.NEW_LINE) {
            result.registerAdvancement()
            this.advance()

            val body = result.register(this.statements())

            if (result.error != null) {
                return result
            }

            body as Node

            if (!this.currentToken.matches(TokenType.KEYWORD, Constants.KEYWORDS["end"])) {
                return result.failure(InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected ${Constants.KEYWORDS["end"]}"
                ))
            }

            result.registerAdvancement()
            this.advance()

            return result.success(ForNode(name, start as Node, end as Node, step, body, true))
        }

        val body = result.register(this.statement())

        if (result.error != null) {
            return result
        }

        return result.success(ForNode(name, start as Node, end as Node, step, body as Node, false))
    }

    private fun whileExpression(): ParseResult {
        val result = ParseResult()

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.KEYWORDS["while"])) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '${Constants.KEYWORDS["while"]}'"
            ))
        }

        result.registerAdvancement()
        this.advance()

        val condition = result.register(this.expression())

        if (result.error != null) {
            return result
        }

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.KEYWORDS["then"])) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '${Constants.KEYWORDS["then"]}'"
            ))
        }

        result.registerAdvancement()
        this.advance()

        if (this.currentToken.type == TokenType.NEW_LINE) {
            result.registerAdvancement()
            this.advance()

            val body = result.register(this.statements())

            if (result.error != null) {
                return result
            }

            if (!this.currentToken.matches(TokenType.KEYWORD, Constants.KEYWORDS["end"])) {
                return result.failure(InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '${Constants.KEYWORDS["end"]}'"
                ))
            }

            result.registerAdvancement()
            this.advance()

            return result.success(WhileNode(condition as Node, body as Node, true))
        }

        val body = result.register(this.statement())

        if (result.error != null) {
            return result
        }

        return result.success(WhileNode(condition as Node, body as Node, false))
    }

    private fun functionDefinition(): ParseResult {
        val result = ParseResult()

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.KEYWORDS["function"])) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected ${Constants.KEYWORDS["function"]}"
            ))
        }

        result.registerAdvancement()
        this.advance()

        val name: Token?

        if (this.currentToken.type == TokenType.IDENTIFIER) {
            name = this.currentToken

            result.registerAdvancement()
            this.advance()

            if (this.currentToken.type != TokenType.LEFT_PARENTHESES) {
                return result.failure(InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '('"
                ))
            }
        } else {
            name = null

            if (this.currentToken.type != TokenType.LEFT_PARENTHESES) {
                return result.failure(InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected identifier or '('"
                ))
            }
        }

        result.registerAdvancement()
        this.advance()

        val argumentNames = arrayListOf<Token>()

        if (this.currentToken.type == TokenType.IDENTIFIER) {
            argumentNames.add(this.currentToken)

            result.registerAdvancement()
            this.advance()

            while (this.currentToken.type == TokenType.COMMA) {
                result.registerAdvancement()
                this.advance()

                if (this.currentToken.type != TokenType.IDENTIFIER) {
                    return result.failure(InvalidSyntaxError(
                        this.currentToken.start,
                        this.currentToken.end,
                        "Expected identifier"
                    ))
                }

                argumentNames.add(this.currentToken)

                result.registerAdvancement()
                this.advance()
            }

            if (this.currentToken.type != TokenType.RIGHT_PARENTHESES) {
                return result.failure(InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected ',' or ')'"
                ))
            }
        } else {
            if (this.currentToken.type != TokenType.RIGHT_PARENTHESES) {
                return result.failure(InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected identifier or ')'"
                ))
            }
        }

        result.registerAdvancement()
        this.advance()

        if (this.currentToken.type == TokenType.ARROW) {
            result.registerAdvancement()
            this.advance()

            val body = result.register(this.expression())

            if (result.error != null) {
                return result
            }

            return result.success(MethodDefineNode(
                name,
                argumentNames,
                this.currentToken,
                body as Node,
                true
            ))
        }

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.KEYWORDS["then"])) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '->' or new line"
            ))
        }

        result.registerAdvancement()
        this.advance()

        var body: Node? = null

        if (!this.getTokenAfter(TokenType.NEW_LINE)!!.matches(TokenType.KEYWORD, Constants.KEYWORDS["end"])) {
            val b = result.register(this.statements())

            if (result.error != null) {
                return result
            }

            body = b as Node
        } else {
            while (this.currentToken.type == TokenType.NEW_LINE) {
                result.registerAdvancement()
                this.advance()
            }
        }

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.KEYWORDS["end"])) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected ${Constants.KEYWORDS["end"]}, got $currentToken"
            ))
        }

        result.registerAdvancement()
        this.advance()

        return result.success(MethodDefineNode(
            name,
            argumentNames,
            this.currentToken,
            body,
            false
        ))
    }

    private fun binaryOperation(function: Supplier<ParseResult>, ops: Array<Any>, functionB: Supplier<ParseResult>? = null): ParseResult {
        val funcB = functionB ?: function

        val result = ParseResult()
        var left = result.register(function.get())

        if (result.error != null) {
            return result
        }

        // don't question it.
        val condition: (token: Token) -> Boolean = { token ->
            if (ops.first() is Pair<*, *>) {
                ops.any { (((it as Pair<*, *>).first) as TokenType) == token.type && ((it.second) as String) == token.value }
            } else {
                ops.any { it == token.type }
            }
        }

        while (condition(this.currentToken)) {
            val operatorToken = this.currentToken

            result.registerAdvancement()
            this.advance()

            val right = result.register(funcB.get())

            if (result.error != null) {
                return result
            }

            left = BinaryOperationNode(left as Node, operatorToken, right as Node)
        }

        return result.success(left as Node)
    }

    private fun getTokenAfter(type: TokenType): Token? {
        val previous = this.tokenIndex

        while (this.currentToken.type == type) {
            this.advance()
        }

        val token = this.currentToken

        while (this.tokenIndex != previous) {
            this.reverse()
        }

        return token
    }

    open class BaseCase(val token: Node, val shouldReturnNull: Boolean)
    class Case(val node: Node, token: Node, shouldReturnNull: Boolean) : BaseCase(token, shouldReturnNull)

}