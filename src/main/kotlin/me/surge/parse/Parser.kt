package me.surge.parse

import me.surge.Constants
import me.surge.lexer.error.impl.ExpectedCharError
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
                "Expected '+', '-', '*', '/', '^', '==', '!=', '<', '>', '<=', '>=', '${Constants.get("and")}' or '${Constants.get("or")}'"
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

        if (this.currentToken.matches(TokenType.KEYWORD, Constants.get("return"))) {
            result.registerAdvancement()
            this.advance()

            val expression = result.tryRegister(this.expression())

            if (expression == null) {
                this.reverse(result.reverseCount)
            }

            return result.success(ReturnNode(expression as Node, start, this.currentToken.end.clone()))
        }

        if (this.currentToken.matches(TokenType.KEYWORD, Constants.get("continue"))) {
            result.registerAdvancement()
            this.advance()
            return result.success(ContinueNode(start, this.currentToken.end.clone()))
        }

        if (this.currentToken.matches(TokenType.KEYWORD, Constants.get("break"))) {
            result.registerAdvancement()
            this.advance()
            return result.success(BreakNode(start, this.currentToken.end.clone()))
        }

        val expression = result.register(this.expression())

        if (result.error != null) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '${Constants.get("return")}', '${Constants.get("continue")}', '${Constants.get("break")}', '${Constants.get("var")}', '${Constants.get("if")}', '${Constants.get("for")}', '${Constants.get("while")}', '${Constants.get("function")}', int, float, identifier, '+', '-', '(', '[' or '${Constants.get("not")}'"
            ))
        }

        return result.success(expression as Node)
    }

    private fun expression(): ParseResult {
        val result = ParseResult()

        if (this.currentToken.matches(TokenType.KEYWORD, Constants.get("var")) || this.currentToken.matches(TokenType.KEYWORD, Constants.get("val"))) {
            val final = this.currentToken.matches(TokenType.KEYWORD, Constants.get("val"))

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

            return result.success(VarAssignNode(name, null, expression!! as Node, declaration = true, final = final))
        }

        val node = result.register(this.binaryOperation({this.comparisonExpression()}, arrayOf(Pair(TokenType.KEYWORD, Constants.get("and")), Pair(TokenType.KEYWORD, Constants.get("or")))))

        if (result.error != null) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '${Constants.get("var")}', '${Constants.get("if")}', '${Constants.get("for")}', '${Constants.get("while")}', int, float, identifier, '+', '-', '(' or '${Constants.get("not")}'"
            ))
        }

        return result.success(node as Node)
    }

    private fun comparisonExpression(): ParseResult {
        val result = ParseResult()

        if (this.currentToken.matches(TokenType.KEYWORD, Constants.get("not"))) {
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
                "Expected int, float, identifier, '+', '-', '(', '${Constants.get("if")}', '${Constants.get("for")}', '${Constants.get("while")}', '${Constants.get("function")}' or '${Constants.get("not")}'"
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
                        "Expected ')', '${Constants.get("var")}', '${Constants.get("if")}', '${Constants.get("for")}', '${Constants.get("while")}', '${Constants.get("function")}', int, float, identifier, '+', '-', '(', or ''${Constants.get("not")}'"
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
            var valueToken: Token = token

            var accessed = false

            result.registerAdvancement()
            this.advance()

            if (this.currentToken.matches(TokenType.KEYWORD, Constants.get("accessor"))) {
                accessed = true

                result.registerAdvancement()
                this.advance()

                valueToken = this.currentToken

                result.registerAdvancement()
                this.advance()
            }

            if (this.currentToken.type == TokenType.EQUALS) {
                result.registerAdvancement()
                this.advance()

                val expression = result.register(this.expression())

                if (result.error != null) {
                    return result
                }

                return result.success(VarAssignNode(if (accessed) valueToken else token, if (accessed) token else null, expression!! as Node, declaration = false, final = false))
            }

            return result.success(VarAccessNode(if (accessed) valueToken else token, if (accessed) token else null))
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

        else if (token.matches(TokenType.KEYWORD, Constants.get("if"))) {
            val ifExpression = result.register(this.ifExpression())

            if (result.error != null) {
                return result
            }

            return result.success(ifExpression as Node)
        }

        else if (token.matches(TokenType.KEYWORD, Constants.get("for"))) {
            val forExpression = result.register(this.forExpression())

            if (result.error != null) {
                return result
            }

            return result.success(forExpression as Node)
        }

        else if (token.matches(TokenType.KEYWORD, Constants.get("while"))) {
            val whileExpression = result.register(this.whileExpression())

            if (result.error != null) {
                return result
            }

            return result.success(whileExpression as Node)
        }

        else if (token.matches(TokenType.KEYWORD, Constants.get("function"))) {
            val definition = result.register(this.functionDefinition())

            if (result.error != null) {
                return result
            }

            return result.success(definition as Node)
        }

        else if (token.matches(TokenType.KEYWORD, Constants.get("struct"))) {
            val definition = result.register(this.structDefinition())

            if (result.error != null) {
                return result
            }

            return result.success(definition as Node)
        }

        else if (token.matches(TokenType.KEYWORD, Constants.get("implement"))) {
            val implementation = result.register(this.structImplementation())

            if (result.error != null) {
                return result
            }

            return result.success(implementation as Node)
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
                    "Expected ']', '${Constants.get("var")}', '${Constants.get("if")}', '${Constants.get("for")}', '${Constants.get("while")}', ${Constants.get("function")}, int, float, identifier, '+', '-', '*', '/', '(', '[' or '${Constants.get("not")}'"
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
        val allCases = result.register(this.ifExpressionCases(Constants.get("if")))

        if (result.error != null) {
            return result
        }

        allCases as Pair<ArrayList<Case>, BaseCase?>

        val cases = allCases.first
        val elseCase = allCases.second

        return result.success(IfNode(cases, elseCase))
    }

    private fun ifExpressionB(): ParseResult {
        return this.ifExpressionCases(Constants.get("elif")!!)
    }

    private fun ifExpressionC(): ParseResult {
        val result = ParseResult()
        var elseCase: BaseCase? = null

        if (this.currentToken.matches(TokenType.KEYWORD, Constants.get("else"))) {
            result.registerAdvancement()
            this.advance()

            if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("then"))) {
                return result.failure(InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '${Constants.get("then")}'"
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

                elseCase = BaseCase(statements as Node, true)

                if (this.currentToken.matches(TokenType.KEYWORD, Constants.get("end"))) {
                    result.registerAdvancement()
                    this.advance()
                } else {
                    return result.failure(InvalidSyntaxError(
                        this.currentToken.start,
                        this.currentToken.end,
                        "Expected '${Constants.get("end")}'"
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

        if (this.currentToken.matches(TokenType.KEYWORD, Constants.get("elif"))) {
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

        skipNewLines(result)

        if (this.currentToken.type != TokenType.LEFT_PARENTHESES) {
            return result.failure(ExpectedCharError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '('"
            ))
        }

        result.registerAdvancement()
        this.advance()

        val condition = result.register(this.expression())

        if (result.error != null) {
            return result
        }

        condition as Node

        if (this.currentToken.type != TokenType.RIGHT_PARENTHESES) {
            return result.failure(ExpectedCharError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected ')'"
            ))
        }

        result.registerAdvancement()
        this.advance()

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("then"))) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected ${Constants.get("then")}"
            ))
        }

        result.registerAdvancement()
        this.advance()

        if (this.currentToken.type == TokenType.NEW_LINE) {
            result.registerAdvancement()
            this.advance()

            skipNewLines(result)

            val statements = result.register(this.statements())

            if (result.error != null) {
                return result
            }

            statements as Node

            cases.add(Case(condition, statements, true))

            if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("end"))) {
                return result.failure(ExpectedCharError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '${Constants.get("end")}'"
                ))
            }

            result.registerAdvancement()
            this.advance()

            if (this.currentToken.matches(TokenType.KEYWORD, Constants.get("elif")) || this.currentToken.matches(TokenType.KEYWORD, Constants.get("else"))) {
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

        result.registerAdvancement()
        this.advance()

        return result.success(Pair(cases, elseCase))
    }

    private fun forExpression(): ParseResult {
        val result = ParseResult()

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("for"))) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '${Constants.get("for")}'"
            ))
        }

        // BEGIN EXPRESSION
        result.registerAdvancement()
        this.advance()

        if (this.currentToken.type != TokenType.LEFT_PARENTHESES) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '('"
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

        if (this.currentToken.matches(TokenType.KEYWORD, Constants.get("in"))) {
            result.registerAdvancement()
            this.advance()

            if (this.currentToken.type != TokenType.IDENTIFIER) {
                return result.failure(InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected identifier"
                ))
            }

            val list = this.currentToken

            result.registerAdvancement()
            this.advance()

            if (this.currentToken.type != TokenType.RIGHT_PARENTHESES) {
                return result.failure(InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected ')'"
                ))
            }

            result.registerAdvancement()
            this.advance()

            if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("then"))) {
                return result.failure(InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '${Constants.get("then")}'"
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

                if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("end"))) {
                    return result.failure(InvalidSyntaxError(
                        this.currentToken.start,
                        this.currentToken.end,
                        "Expected ${Constants.get("end")}"
                    ))
                }

                result.registerAdvancement()
                this.advance()

                return result.success(IterationNode(name, list, body, true))
            }

            val body = result.register(this.statement())

            if (result.error != null) {
                return result
            }

            return result.success(IterationNode(name, list, body as Node, false))
        }

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

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("to"))) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '${Constants.get("to")}'"
            ))
        }

        result.registerAdvancement()
        this.advance()

        val end = result.register(this.expression())

        if (result.error != null) {
            return result
        }

        var step: Node? = null

        if (this.currentToken.matches(TokenType.KEYWORD, Constants.get("step"))) {
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

            val s = result.register(this.expression())

            if (result.error != null) {
                return result
            }

            step = s as Node
        }

        if (this.currentToken.type != TokenType.RIGHT_PARENTHESES) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected ')'"
            ))
        }

        result.registerAdvancement()
        this.advance()

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("then"))) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '${Constants.get("then")}'"
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

            if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("end"))) {
                return result.failure(InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected ${Constants.get("end")}"
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

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("while"))) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '${Constants.get("while")}'"
            ))
        }

        result.registerAdvancement()
        this.advance()

        if (this.currentToken.type != TokenType.LEFT_PARENTHESES) {
            return result.failure(ExpectedCharError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '('"
            ))
        }

        result.registerAdvancement()
        this.advance()

        val condition = result.register(this.expression())

        if (result.error != null) {
            return result
        }

        if (this.currentToken.type != TokenType.RIGHT_PARENTHESES) {
            return result.failure(ExpectedCharError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected ')'"
            ))
        }

        result.registerAdvancement()
        this.advance()

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("then"))) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '${Constants.get("then")}'"
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

            if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("end"))) {
                return result.failure(InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '${Constants.get("end")}'"
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

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("function"))) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected ${Constants.get("function")}"
            ))
        }

        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

        val name: Token?

        if (this.currentToken.type == TokenType.IDENTIFIER) {
            name = this.currentToken

            result.registerAdvancement()
            this.advance()

            skipNewLines(result)

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

        skipNewLines(result)

        val argumentNames = arrayListOf<Token>()

        if (this.currentToken.type == TokenType.IDENTIFIER) {
            argumentNames.add(this.currentToken)

            result.registerAdvancement()
            this.advance()

            skipNewLines(result)

            while (this.currentToken.type == TokenType.COMMA) {
                result.registerAdvancement()
                this.advance()

                skipNewLines(result)

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

                skipNewLines(result)
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

        skipNewLines(result)

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

        skipNewLines(result)

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("then"))) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '->' or ${Constants.get("then")}"
            ))
        }

        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

        val body = result.register(this.statements())

        if (result.error != null) {
            return result
        }

        body as Node

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("end"))) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected ${Constants.get("end")}, got $currentToken"
            ))
        }

        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

        return result.success(MethodDefineNode(
            name,
            argumentNames,
            this.currentToken,
            body,
            false
        ))
    }

    private fun structDefinition(): ParseResult {
        val result = ParseResult()

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("struct"))) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '${Constants.get("function")}'"
            ))
        }

        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

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

        skipNewLines(result)

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("then"))) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '${Constants.get("then")}'"
            ))
        }

        skipNewLines(result)

        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

        val argumentNames = arrayListOf<Token>()

        if (this.currentToken.type == TokenType.IDENTIFIER) {
            argumentNames.add(this.currentToken)

            result.registerAdvancement()
            this.advance()

            skipNewLines(result)

            while (this.currentToken.type == TokenType.COMMA) {
                result.registerAdvancement()
                this.advance()

                skipNewLines(result)

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

                skipNewLines(result)
            }

            skipNewLines(result)

            if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("end"))) {
                return result.failure(InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected ',' or '${Constants.get("end")}'"
                ))
            }
        } else {
            skipNewLines(result)

            if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("end"))) {
                return result.failure(InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected identifier or '${Constants.get("end")}'"
                ))
            }
        }

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("end"))) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected ${Constants.get("end")}, got $currentToken"
            ))
        }

        result.registerAdvancement()
        this.advance()

        return result.success(StructDefineNode(name, argumentNames, this.currentToken))
    }

    private fun structImplementation(): ParseResult {
        val result = ParseResult()

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("implement"))) {
            return result.failure(InvalidSyntaxError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '${Constants.get("implement")}'"
            ))
        }

        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

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

        skipNewLines(result)

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("then"))) {
            return result.failure(ExpectedCharError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '${Constants.get("then")}'"
            ))
        }

        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

        val bodyNodes = arrayListOf<Node>()

        while (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("end"))) {
            val body = result.register(this.statements())

            if (result.error != null) {
                return result
            }

            body as Node

            bodyNodes.add(body)
        }

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("end"))) {
            return result.failure(ExpectedCharError(
                this.currentToken.start,
                this.currentToken.end,
                "Expected '${Constants.get("end")}'"
            ))
        }

        result.registerAdvancement()
        this.advance()

        return result.success(StructImplementationNode(
            name,
            ListNode(bodyNodes, this.currentToken.start, this.currentToken.end),
            this.currentToken
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

    private fun skipNewLines(result: ParseResult) {
        while (this.currentToken.type == TokenType.NEW_LINE) {
            result.registerAdvancement()
            this.advance()
        }
    }

    open class BaseCase(val token: Node, val shouldReturnNull: Boolean)
    class Case(val node: Node, token: Node, shouldReturnNull: Boolean) : BaseCase(token, shouldReturnNull)

}