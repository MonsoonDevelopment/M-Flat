package me.surge.lang.parse

import me.surge.lang.error.impl.InvalidSyntaxError
import me.surge.lang.node.*
import me.surge.lang.lexer.token.Token
import me.surge.lang.lexer.token.TokenType
import me.surge.lang.util.Constants
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
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '+', '-', '*', '/', '^', '==', '!=', '<', '>', '<=', '>=', '${Constants.get("and")}' or '${
                        Constants.get(
                            "or"
                        )
                    }'"
                )
            )
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

        val expr = this.statement()
        val statement = result.tryRegister(expr)

        if (expr.error != null && this.currentToken.type != TokenType.EOF && !this.currentToken.matches(TokenType.KEYWORD, Constants.get("end"))) {
            return expr
        }

        if (statement != null) {
            statements.add(statement as Node)
        }

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

            val expr = this.statement()
            val statement = result.tryRegister(expr)

            if (expr.error != null && this.currentToken.type != TokenType.EOF && !this.currentToken.matches(TokenType.KEYWORD, Constants.get("end"))) {
                return expr
            }

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

            return result.success(ReturnNode(expression as Node?, start, this.currentToken.end.clone()))
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
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '${Constants.get("return")}', '${Constants.get("continue")}', '${Constants.get("break")}', '${
                        Constants.get(
                            "var"
                        )
                    }', '${Constants.get("if")}', '${Constants.get("for")}', '${Constants.get("while")}', '${
                        Constants.get(
                            "function"
                        )
                    }', int, float, identifier, '+', '-', '(', '[' or '${Constants.get("not")}'"
                )
            )
        }

        return result.success(expression as Node)
    }

    private fun expression(): ParseResult {
        val result = ParseResult()

        if (this.currentToken.matches(TokenType.KEYWORD, Constants.get("var")) || this.currentToken.matches(TokenType.KEYWORD, Constants.get("val"))) {
            val final = this.currentToken.matches(TokenType.KEYWORD, Constants.get("val"))

            result.registerAdvancement()
            this.advance()

            skipNewLines(result)

            if (this.currentToken.type != TokenType.IDENTIFIER) {
                return result.failure(
                    InvalidSyntaxError(
                        this.currentToken.start,
                        this.currentToken.end,
                        "Expected identifier"
                    )
                )
            }

            val name = this.currentToken

            result.registerAdvancement()
            this.advance()

            skipNewLines(result)

            if (this.currentToken.type != TokenType.EQUALS) {
                return result.failure(
                    InvalidSyntaxError(
                        this.currentToken.start,
                        this.currentToken.end,
                        "Expected '='"
                    )
                )
            }

            result.registerAdvancement()
            this.advance()

            skipNewLines(result)

            val expression = result.register(this.expression())

            if (result.error != null) {
                return result
            }

            return result.success(VarAssignNode(name, expression!! as Node, declaration = true, final = final))
        }

        val node = result.register(this.binaryOperation({this.comparisonExpression()}, arrayOf(Pair(TokenType.KEYWORD, Constants.get("and")), Pair(TokenType.KEYWORD, Constants.get("or")))))

        if (result.error != null) {
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '${Constants.get("var")}', '${Constants.get("if")}', '${Constants.get("for")}', '${
                        Constants.get(
                            "while"
                        )
                    }', int, float, identifier, '+', '-', '(' or '${Constants.get("not")}'"
                )
            )
        }

        return result.success(node as Node)
    }

    private fun comparisonExpression(): ParseResult {
        val result = ParseResult()

        if (this.currentToken.matches(TokenType.KEYWORD, Constants.get("not"))) {
            val operator = this.currentToken

            result.registerAdvancement()
            this.advance()

            skipNewLines(result)

            val node = result.register(this.comparisonExpression()) as Node

            if (result.error != null) {
                return result
            }

            return result.success(UnaryOperationNode(operator, node))
        }

        val node = result.register(this.binaryOperation({this.arithmeticExpression()}, arrayOf(TokenType.EQUALITY, TokenType.INEQUALITY, TokenType.LESS_THAN, TokenType.GREATER_THAN, TokenType.LESS_THAN_OR_EQUAL_TO, TokenType.GREATER_THAN_OR_EQUAL_TO)))

        if (result.error != null) {
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected int, float, identifier, '+', '-', '(', '${Constants.get("if")}', '${Constants.get("for")}', '${
                        Constants.get(
                            "while"
                        )
                    }', '${Constants.get("function")}' or '${Constants.get("not")}'"
                )
            )
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

            skipNewLines(result)

            val factor = result.register(this.factor())

            if (result.error != null) {
                return result
            }

            return result.success(UnaryOperationNode(token, factor as Node))
        }

        return this.modulo()
    }

    private fun modulo(): ParseResult {
        return this.binaryOperation({ this.call() }, arrayOf(TokenType.MODULO), { this.factor() })
    }

    private fun call(): ParseResult {
        val result = ParseResult()
        val atom = result.register(this.atom())

        if (result.error != null) {
            return result
        }

        val index = this.tokenIndex

        skipNewLines(result)

        if (this.currentToken.type == TokenType.LEFT_PARENTHESES) {
            result.registerAdvancement()
            this.advance()

            skipNewLines(result)

            val argumentNodes = arrayListOf<Node>()

            if (this.currentToken.type == TokenType.RIGHT_PARENTHESES) {
                result.registerAdvancement()
                this.advance()
            } else {
                val node = result.register(this.expression())

                if (result.error != null) {
                    return result.failure(
                        InvalidSyntaxError(
                            this.currentToken.start,
                            this.currentToken.end,
                            "Expected ')', '${Constants.get("var")}', '${Constants.get("if")}', '${Constants.get("for")}', '${
                                Constants.get(
                                    "while"
                                )
                            }', '${Constants.get("function")}', int, float, identifier, '+', '-', '(', or ''${
                                Constants.get(
                                    "not"
                                )
                            }'"
                        )
                    )
                }

                argumentNodes.add(node as Node)

                while (this.currentToken.type == TokenType.COMMA) {
                    result.registerAdvancement()
                    this.advance()

                    skipNewLines(result)

                    val node = result.register(this.expression())

                    if (result.error != null) {
                        return result
                    }

                    argumentNodes.add(node as Node)
                }

                skipNewLines(result)

                if (this.currentToken.type != TokenType.RIGHT_PARENTHESES) {
                    return result.failure(
                        InvalidSyntaxError(
                            this.currentToken.start,
                            this.currentToken.end,
                            "Expected ',' or ')'"
                        )
                    )
                }

                result.registerAdvancement()
                this.advance()
            }

            var child: Node? = null

            while (this.currentToken.matches(TokenType.KEYWORD, Constants.get("accessor"))) {
                if (child == null) {
                    child = atom as Node
                }

                result.registerAdvancement()
                this.advance()

                val sub = result.register(this.call())

                if (result.error != null) {
                    return result
                }

                child.child = sub as Node
                child = sub
            }

            var indexNode: Node? = null

            if (this.currentToken.type == TokenType.LEFT_SQUARE) {
                result.registerAdvancement()
                this.advance()

                skipNewLines(result)

                val index = result.register(this.expression())

                if (result.error != null) {
                    return result
                }

                indexNode = index as Node

                if (this.currentToken.type != TokenType.RIGHT_SQUARE) {
                    return result.failure(
                        InvalidSyntaxError(
                            indexNode.start,
                            indexNode.end,
                            "Expected ']'"
                        )
                    )
                }

                result.registerAdvancement()
                this.advance()
            }

            return result.success(MethodCallNode(atom as Node, argumentNodes, child, indexNode))
        } else {
            this.reverse(this.tokenIndex - index)
        }

        var child: Node? = null

        while (this.currentToken.matches(TokenType.KEYWORD, Constants.get("accessor"))) {
            if (child == null) {
                child = atom as Node
            }

            result.registerAdvancement()
            this.advance()

            val sub = result.register(this.call())

            if (result.error != null) {
                return result
            }

            child.child = sub as Node
            child = sub
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

            var start: Node? = null
            var end: Node? = null

            if (this.currentToken.type == TokenType.LEFT_SQUARE) {
                result.registerAdvancement()
                this.advance()

                skipNewLines(result)

                val startExpression = result.register(this.expression())

                if (result.error != null) {
                    return result
                }

                start = startExpression as Node

                val index = this.tokenIndex
                skipNewLines(result)

                if (this.currentToken.matches(TokenType.KEYWORD, Constants.get("index splitter"))) {
                    result.registerAdvancement()
                    this.advance()

                    skipNewLines(result)

                    if (this.currentToken.type == TokenType.RIGHT_SQUARE) {
                        end = EndNode(this.currentToken.start, this.currentToken.end)
                    } else {
                        val endExpression = result.register(this.expression())

                        if (result.error != null) {
                            return result
                        }

                        end = endExpression as Node

                        skipNewLines(result)
                    }

                    if (this.currentToken.type != TokenType.RIGHT_SQUARE) {
                        return result.failure(
                            me.surge.lang.error.impl.ExpectedCharError(
                                this.currentToken.start,
                                this.currentToken.end,
                                "Expected ']', got ${this.currentToken}!"
                            )
                        )
                    }

                    result.registerAdvancement()
                    this.advance()
                } else {
                    end = start
                    this.reverse(this.tokenIndex - index)
                    result.registerAdvancement()
                    this.advance()
                }
            }

            return result.success(StringNode(token, start, end))
        }

        else if (token.type == TokenType.IDENTIFIER) {
            result.registerAdvancement()
            this.advance()

            val index = this.tokenIndex

            skipNewLines(result)

            if (this.currentToken.type == TokenType.EQUALS) {
                result.registerAdvancement()
                this.advance()

                skipNewLines(result)

                val expression = result.register(this.expression())

                if (result.error != null) {
                    return result
                }

                return result.success(VarAssignNode(token, expression!! as Node, declaration = false, final = false))
            } else if (this.currentToken.type == TokenType.ADD || this.currentToken.type == TokenType.SUBTRACT_BY || this.currentToken.type == TokenType.MULTIPLY_BY || this.currentToken.type == TokenType.DIVIDE_BY) {
                val type = this.currentToken.type

                result.registerAdvancement()
                this.advance()

                val expression = result.register(this.expression())

                if (result.error != null) {
                    return result
                }

                val node = VarAssignNode(token, expression!! as Node, declaration = false, final = false, mutate = type)

                return result.success(node)
            } else {
                this.reverse(this.tokenIndex - index)
            }

            var indexNode: Node? = null

            if (this.currentToken.type == TokenType.LEFT_SQUARE) {
                result.registerAdvancement()
                this.advance()

                skipNewLines(result)

                val index = result.register(this.expression())

                if (result.error != null) {
                    return result
                }

                indexNode = index as Node

                if (this.currentToken.type != TokenType.RIGHT_SQUARE) {
                    return result.failure(
                        InvalidSyntaxError(
                            indexNode.start,
                            indexNode.end,
                            "Expected ']'"
                        )
                    )
                }

                result.registerAdvancement()
                this.advance()
            }

            return result.success(VarAccessNode(token, indexNode))
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
                result.failure(
                    InvalidSyntaxError(
                        this.currentToken.start,
                        this.currentToken.end,
                        "Expected ')'"
                    )
                )
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
            val definition = result.register(this.methodDefinition())

            if (result.error != null) {
                return result
            }

            return result.success(definition as Node)
        }

        else if (token.matches(TokenType.KEYWORD, Constants.get("container"))) {
            val definition = result.register(this.containerDefinition())

            if (result.error != null) {
                return result
            }

            return result.success(definition as Node)
        }

        else if (token.matches(TokenType.KEYWORD, Constants.get("import"))) {
            val use = result.register(this.use())

            if (result.error != null) {
                return result
            }

            return result.success(use as Node)
        }

        else if (token.matches(TokenType.KEYWORD, Constants.get("enum"))) {
            val definition = result.register(this.enumDefinition())

            if (result.error != null) {
                return result
            }

            return result.success(definition as Node)
        }

        return result.failure(
            InvalidSyntaxError(
                token.start,
                token.end,
                "Expected int, float, '+', '-' or '('"
            )
        )
    }

    private fun listExpression(): ParseResult {
        val result = ParseResult()
        val elementNodes = arrayListOf<Node>()
        val start = this.currentToken.start.clone()

        if (this.currentToken.type != TokenType.LEFT_SQUARE) {
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '['"
                )
            )
        }

        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

        if (this.currentToken.type == TokenType.RIGHT_SQUARE) {
            result.registerAdvancement()
            this.advance()
        } else {
            val node = result.register(this.expression())

            if (result.error != null) {
                return result.failure(
                    InvalidSyntaxError(
                        this.currentToken.start,
                        this.currentToken.end,
                        "Expected ']', '${Constants.get("var")}', '${Constants.get("if")}', '${Constants.get("for")}', '${
                            Constants.get(
                                "while"
                            )
                        }', ${Constants.get("function")}, int, float, identifier, '+', '-', '*', '/', '(', '[' or '${
                            Constants.get(
                                "not"
                            )
                        }'"
                    )
                )
            }

            elementNodes.add(node as Node)

            skipNewLines(result)

            while (this.currentToken.type == TokenType.COMMA) {
                result.registerAdvancement()
                this.advance()

                skipNewLines(result)

                val node = result.register(this.expression())

                if (result.error != null) {
                    return result
                }

                elementNodes.add(node as Node)
            }

            skipNewLines(result)

            if (this.currentToken.type != TokenType.RIGHT_SQUARE) {
                return result.failure(
                    InvalidSyntaxError(
                        this.currentToken.start,
                        this.currentToken.end,
                        "Expected ',' or ']'"
                    )
                )
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
        return this.ifExpressionCases(Constants.get("elif"))
    }

    private fun ifExpressionC(): ParseResult {
        val result = ParseResult()
        var elseCase: BaseCase? = null

        if (this.currentToken.matches(TokenType.KEYWORD, Constants.get("else"))) {
            result.registerAdvancement()
            this.advance()

            skipNewLines(result)

            if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("then"))) {
                return result.failure(
                    InvalidSyntaxError(
                        this.currentToken.start,
                        this.currentToken.end,
                        "Expected '${Constants.get("then")}'"
                    )
                )
            }

            result.registerAdvancement()
            this.advance()

            skipNewLines(result)

            val statements = result.register(this.statements())

            if (result.error != null) {
                return result
            }

            elseCase = BaseCase(statements as Node, true)

            if (this.currentToken.matches(TokenType.KEYWORD, Constants.get("end"))) {
                result.registerAdvancement()
                this.advance()
            } else {
                return result.failure(
                    InvalidSyntaxError(
                        this.currentToken.start,
                        this.currentToken.end,
                        "Expected '${Constants.get("end")}'"
                    )
                )
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
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '$keyword'"
                )
            )
        }

        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

        if (this.currentToken.type != TokenType.LEFT_PARENTHESES) {
            return result.failure(
                me.surge.lang.error.impl.ExpectedCharError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '('"
                )
            )
        }

        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

        val condition = result.register(this.expression())

        if (result.error != null) {
            return result
        }

        condition as Node

        skipNewLines(result)

        if (this.currentToken.type != TokenType.RIGHT_PARENTHESES) {
            return result.failure(
                me.surge.lang.error.impl.ExpectedCharError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected ')'"
                )
            )
        }

        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("then"))) {
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected ${Constants.get("then")}"
                )
            )
        }

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
            return result.failure(
                me.surge.lang.error.impl.ExpectedCharError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '${Constants.get("end")}'"
                )
            )
        }

        result.registerAdvancement()
        this.advance()

        val index = this.tokenIndex

        skipNewLines(result)

        if (this.currentToken.matches(TokenType.KEYWORD, Constants.get("elif")) || this.currentToken.matches(TokenType.KEYWORD, Constants.get("else"))) {
            val allCases = result.register(this.ifExpressionBorC())

            if (result.error != null) {
                return result
            }

            allCases as Pair<ArrayList<Case>, BaseCase?>

            val new = allCases.first
            elseCase = allCases.second

            cases.addAll(new)
        } else {
            this.reverse(this.tokenIndex - index)
        }

        result.registerAdvancement()
        this.advance()

        return result.success(Pair(cases, elseCase))
    }

    private fun forExpression(): ParseResult {
        val result = ParseResult()

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("for"))) {
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '${Constants.get("for")}'"
                )
            )
        }

        // BEGIN EXPRESSION
        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

        if (this.currentToken.type != TokenType.LEFT_PARENTHESES) {
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '('"
                )
            )
        }

        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

        if (this.currentToken.type != TokenType.IDENTIFIER) {
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected identifier"
                )
            )
        }

        val name = this.currentToken
        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

        if (this.currentToken.matches(TokenType.KEYWORD, Constants.get("in"))) {
            result.registerAdvancement()
            this.advance()

            skipNewLines(result)

            if (this.currentToken.type != TokenType.IDENTIFIER) {
                return result.failure(
                    InvalidSyntaxError(
                        this.currentToken.start,
                        this.currentToken.end,
                        "Expected identifier"
                    )
                )
            }

            val list = this.currentToken

            result.registerAdvancement()
            this.advance()

            skipNewLines(result)

            if (this.currentToken.type != TokenType.RIGHT_PARENTHESES) {
                return result.failure(
                    InvalidSyntaxError(
                        this.currentToken.start,
                        this.currentToken.end,
                        "Expected ')'"
                    )
                )
            }

            result.registerAdvancement()
            this.advance()

            skipNewLines(result)

            if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("then"))) {
                return result.failure(
                    InvalidSyntaxError(
                        this.currentToken.start,
                        this.currentToken.end,
                        "Expected '${Constants.get("then")}'"
                    )
                )
            }

            result.registerAdvancement()
            this.advance()

            val body = result.register(this.statements())

            if (result.error != null) {
                return result
            }

            body as Node

            if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("end"))) {
                return result.failure(
                    InvalidSyntaxError(
                        this.currentToken.start,
                        this.currentToken.end,
                        "Expected ${Constants.get("end")}"
                    )
                )
            }

            result.registerAdvancement()
            this.advance()

            return result.success(IterationNode(name, list, body, true))
        }

        if (this.currentToken.type != TokenType.EQUALS) {
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '='"
                )
            )
        }

        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

        val start = result.register(this.expression())

        if (result.error != null) {
            return result
        }

        skipNewLines(result)

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("to"))) {
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '${Constants.get("to")}'"
                )
            )
        }

        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

        val end = result.register(this.expression())

        if (result.error != null) {
            return result
        }

        var step: Node? = null

        skipNewLines(result)

        if (this.currentToken.type == TokenType.COMMA) {
            result.registerAdvancement()
            this.advance()

            if (this.currentToken.matches(TokenType.KEYWORD, Constants.get("step"))) {
                result.registerAdvancement()
                this.advance()

                skipNewLines(result)

                if (this.currentToken.type != TokenType.EQUALS) {
                    return result.failure(
                        InvalidSyntaxError(
                            this.currentToken.start,
                            this.currentToken.end,
                            "Expected '='"
                        )
                    )
                }

                result.registerAdvancement()
                this.advance()

                skipNewLines(result)

                val s = result.register(this.expression())

                if (result.error != null) {
                    return result
                }

                step = s as Node

                skipNewLines(result)
            } else {
                return result.failure(
                    InvalidSyntaxError(
                        this.currentToken.start,
                        this.currentToken.end,
                        "Expected '${Constants.get("step")}'"
                    )
                )
            }
        }

        if (this.currentToken.type != TokenType.RIGHT_PARENTHESES) {
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected ')'"
                )
            )
        }

        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("then"))) {
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '${Constants.get("then")}'"
                )
            )
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
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected ${Constants.get("end")}"
                )
            )
        }

        result.registerAdvancement()
        this.advance()

        return result.success(ForNode(name, start as Node, end as Node, step, body, true))
    }

    private fun whileExpression(): ParseResult {
        val result = ParseResult()

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("while"))) {
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '${Constants.get("while")}'"
                )
            )
        }

        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

        if (this.currentToken.type != TokenType.LEFT_PARENTHESES) {
            return result.failure(
                me.surge.lang.error.impl.ExpectedCharError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '('"
                )
            )
        }

        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

        val condition = result.register(this.expression())

        if (result.error != null) {
            return result
        }

        skipNewLines(result)

        if (this.currentToken.type != TokenType.RIGHT_PARENTHESES) {
            return result.failure(
                me.surge.lang.error.impl.ExpectedCharError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected ')'"
                )
            )
        }

        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("then"))) {
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '${Constants.get("then")}'"
                )
            )
        }

        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

        val body = result.register(this.statements())

        if (result.error != null) {
            return result
        }

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("end"))) {
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '${Constants.get("end")}'"
                )
            )
        }

        result.registerAdvancement()
        this.advance()

        return result.success(WhileNode(condition as Node, body as Node, true))
    }

    private fun methodDefinition(): ParseResult {
        val result = ParseResult()

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("function"))) {
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected ${Constants.get("function")}"
                )
            )
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
        } else {
            name = null
        }

        val argumentNames = arrayListOf<Token>()

        if (this.currentToken.type == TokenType.LEFT_PARENTHESES) {
            result.registerAdvancement()
            this.advance()

            skipNewLines(result)

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
                        return result.failure(
                            InvalidSyntaxError(
                                this.currentToken.start,
                                this.currentToken.end,
                                "Expected identifier"
                            )
                        )
                    }

                    argumentNames.add(this.currentToken)

                    result.registerAdvancement()
                    this.advance()

                    skipNewLines(result)
                }

                if (this.currentToken.type != TokenType.RIGHT_PARENTHESES) {
                    return result.failure(
                        InvalidSyntaxError(
                            this.currentToken.start,
                            this.currentToken.end,
                            "Expected ',' or ')', got $currentToken"
                        )
                    )
                }
            } else {
                if (this.currentToken.type != TokenType.RIGHT_PARENTHESES) {
                    return result.failure(
                        InvalidSyntaxError(
                            this.currentToken.start,
                            this.currentToken.end,
                            "Expected identifier or ')', got $currentToken"
                        )
                    )
                }
            }

            result.registerAdvancement()
            this.advance()

            skipNewLines(result)
        }

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
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '->' or ${Constants.get("then")}, got $currentToken"
                )
            )
        }

        result.registerAdvancement()
        this.advance()

        val body = result.register(this.statements())

        if (result.error != null) {
            return result
        }

        body as Node

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("end"))) {
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Method Definition: Expected ${Constants.get("end")}, got $currentToken"
                )
            )
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

    private fun containerDefinition(): ParseResult {
        val result = ParseResult()

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("container"))) {
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '${Constants.get("container")}'"
                )
            )
        }

        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

        if (this.currentToken.type != TokenType.IDENTIFIER) {
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected identifier"
                )
            )
        }

        val name = this.currentToken

        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

        var argumented = false
        var bodied = false

        val argumentNames = hashMapOf<Int, ArrayList<ArgumentToken>>()

        while (this.currentToken.type == TokenType.LEFT_PARENTHESES) {
            val start = this.currentToken
            val constructor = arrayListOf<ArgumentToken>()

            argumented = true

            result.registerAdvancement()
            this.advance()

            skipNewLines(result)

            if (this.currentToken.type == TokenType.IDENTIFIER) {
                var optionals = false

                val name = this.currentToken
                var defaultValue: Node? = null

                result.registerAdvancement()
                this.advance()

                skipNewLines(result)

                if (this.currentToken.type == TokenType.EQUALS) {
                    optionals = true

                    result.registerAdvancement()
                    this.advance()

                    skipNewLines(result)

                    val node = result.register(this.expression())

                    if (result.error != null) {
                        return result
                    }

                    defaultValue = node as Node?

                    skipNewLines(result)
                }

                constructor.add(ArgumentToken(name, defaultValue))

                while (this.currentToken.type == TokenType.COMMA) {
                    result.registerAdvancement()
                    this.advance()

                    skipNewLines(result)

                    if (this.currentToken.type != TokenType.IDENTIFIER) {
                        return result.failure(
                            InvalidSyntaxError(
                                this.currentToken.start,
                                this.currentToken.end,
                                "Expected identifier"
                            )
                        )
                    }

                    val name = this.currentToken
                    var defaultValue: Node? = null

                    result.registerAdvancement()
                    this.advance()

                    skipNewLines(result)

                    if (optionals && this.currentToken.type != TokenType.EQUALS) {
                        return result.failure(
                            InvalidSyntaxError(
                                this.currentToken.start,
                                this.currentToken.end,
                                "Argument '${name.value as String}' must be optional as previous arguments have been optional!"
                            )
                        )
                    }

                    if (this.currentToken.type == TokenType.EQUALS) {
                        optionals = true

                        result.registerAdvancement()
                        this.advance()

                        skipNewLines(result)

                        val node = result.register(this.expression())

                        if (result.error != null) {
                            return result
                        }

                        defaultValue = node as Node?

                        skipNewLines(result)
                    }

                    constructor.add(ArgumentToken(name, defaultValue))

                    skipNewLines(result)
                }

                skipNewLines(result)

                if (this.currentToken.type != TokenType.RIGHT_PARENTHESES) {
                    return result.failure(
                        InvalidSyntaxError(
                            this.currentToken.start,
                            this.currentToken.end,
                            "Expected ',' or ')', got $currentToken"
                        )
                    )
                }
            } else {
                skipNewLines(result)

                if (this.currentToken.type != TokenType.RIGHT_PARENTHESES) {
                    return result.failure(
                        InvalidSyntaxError(
                            this.currentToken.start,
                            this.currentToken.end,
                            "Expected identifier or ')'"
                        )
                    )
                }
            }

            if (this.currentToken.type != TokenType.RIGHT_PARENTHESES) {
                return result.failure(
                    InvalidSyntaxError(
                        this.currentToken.start,
                        this.currentToken.end,
                        "Container Definition: Expected ')', got $currentToken"
                    )
                )
            }

            if (argumentNames[constructor.size] != null) {
                return result.failure(
                    me.surge.lang.error.impl.DualConstructorError(
                        start.start,
                        this.currentToken.end,
                        "Constructor with ${constructor.size} arguments was already defined for container '${name.value as String}'"
                    )
                )
            }

            result.registerAdvancement()
            this.advance()

            skipNewLines(result)

            argumentNames[constructor.size] = constructor
        }

        var body: Node? = null

        if (this.currentToken.matches(TokenType.KEYWORD, Constants.get("then"))) {
            bodied = true

            result.registerAdvancement()
            this.advance()

            val bodyStatements = result.register(this.statements())

            if (result.error != null) {
                return result
            }

            body = bodyStatements as Node

            if (this.currentToken.matches(TokenType.KEYWORD, Constants.get("end"))) {
                result.registerAdvancement()
                this.advance()
            } else {
                return result.failure(
                    me.surge.lang.error.impl.ExpectedCharError(
                        this.currentToken.start,
                        this.currentToken.end,
                        "Expected '${Constants.get("end")}'"
                    )
                )
            }
        }

        if (!argumented && !bodied) {
            return result.failure(
                InvalidSyntaxError(
                    name.start,
                    name.end,
                    "Container needs to have either arguments ('()') or a body ('{}')"
                )
            )
        }

        return result.success(ContainerDefinitionNode(name, argumentNames, this.currentToken, body))
    }

    private fun enumDefinition(): ParseResult {
        val result = ParseResult()

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("enum"))) {
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '${Constants.get("enum")}', got $currentToken"
                )
            )
        }

        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

        if (this.currentToken.type != TokenType.IDENTIFIER) {
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected identifier, got $currentToken"
                )
            )
        }

        val name = this.currentToken

        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

        var memberArguments = arrayListOf<ArgumentToken>()

        if (this.currentToken.type == TokenType.LEFT_PARENTHESES) {
            memberArguments = generateArguments(result)

            if (result.error != null) {
                return result
            }
        }

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("then"))) {
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected ${Constants.get("then")}, got $currentToken"
                )
            )
        }

        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

        if (this.currentToken.matches(TokenType.KEYWORD, Constants.get("end"))) {
            return result.success(
                EnumDefinitionNode(
                    name,
                    memberArguments,
                    linkedMapOf(),
                    null,
                    name.start,
                    this.currentToken.end
                )
            )
        }

        val members = linkedMapOf<Token, List<Node>>()

        while (this.currentToken.type == TokenType.IDENTIFIER) {
            val memberName = this.currentToken

            result.registerAdvancement()
            this.advance()

            skipNewLines(result)

            val args = arrayListOf<Node>()

            /*if (memberArguments.isNotEmpty()) {
                if (this.currentToken.type != TokenType.LEFT_PARENTHESES) {
                    return result.failure(InvalidSyntaxError(
                        this.currentToken.start,
                        this.currentToken.end,
                        "Expected '(', got $currentToken"
                    ))
                }

                result.registerAdvancement()
                this.advance()

                skipNewLines(result)

                val node = result.register(this.expression())

                if (result.error != null) {
                    return result
                }

                args.add(node as Node)

                skipNewLines(result)

                while (this.currentToken.type == TokenType.COMMA) {
                    result.registerAdvancement()
                    this.advance()

                    skipNewLines(result)

                    val node = result.register(this.expression())

                    if (result.error != null) {
                        return result
                    }

                    args.add(node as Node)

                    skipNewLines(result)
                }

                if (this.currentToken.type != TokenType.RIGHT_PARENTHESES) {
                    return result.failure(InvalidSyntaxError(
                        this.currentToken.start,
                        this.currentToken.end,
                        "Expected ',' or ')', got $currentToken"
                    ))
                }

                result.registerAdvancement()
                this.advance()
            } else {

            }*/

            if (this.currentToken.type == TokenType.LEFT_PARENTHESES) {
                result.registerAdvancement()
                this.advance()

                skipNewLines(result)

                if (this.currentToken.type == TokenType.RIGHT_PARENTHESES) {
                    result.registerAdvancement()
                    this.advance()

                    skipNewLines(result)
                } else {
                    val node = result.register(this.expression())

                    if (result.error != null) {
                        return result
                    }

                    args.add(node as Node)

                    skipNewLines(result)

                    while (this.currentToken.type == TokenType.COMMA) {
                        result.registerAdvancement()
                        this.advance()

                        skipNewLines(result)

                        val node = result.register(this.expression())

                        if (result.error != null) {
                            return result
                        }

                        args.add(node as Node)

                        skipNewLines(result)
                    }

                    if (this.currentToken.type != TokenType.RIGHT_PARENTHESES) {
                        return result.failure(
                            InvalidSyntaxError(
                                this.currentToken.start,
                                this.currentToken.end,
                                "Expected ',' or ')', got $currentToken"
                            )
                        )
                    }

                    result.registerAdvancement()
                    this.advance()

                    skipNewLines(result)
                }
            }

            if (this.currentToken.type == TokenType.COMMA) {
                result.registerAdvancement()
                this.advance()
            }

            members[memberName] = args

            skipNewLines(result)
        }

        var body: Node? = null

        if (this.currentToken.type == TokenType.KEYWORD && !this.currentToken.matches(TokenType.KEYWORD, Constants.get("end"))) {
            val bodyStatements = result.register(this.statements())

            if (result.error != null) {
                return result
            }

            body = bodyStatements as Node
        }

        result.registerAdvancement()
        this.advance()

        return result.success(
            EnumDefinitionNode(
                name,
                memberArguments,
                members,
                body,
                name.start,
                this.currentToken.end
            )
        )
    }

    private fun generateArguments(result: ParseResult): ArrayList<ArgumentToken> {
        val argumentNames = arrayListOf<ArgumentToken>()

        while (this.currentToken.type == TokenType.LEFT_PARENTHESES) {
            result.registerAdvancement()
            this.advance()

            skipNewLines(result)

            if (this.currentToken.type == TokenType.IDENTIFIER) {
                var optionals = false

                val name = this.currentToken
                var defaultValue: Node? = null

                result.registerAdvancement()
                this.advance()

                skipNewLines(result)

                if (this.currentToken.type == TokenType.EQUALS) {
                    optionals = true

                    result.registerAdvancement()
                    this.advance()

                    skipNewLines(result)

                    val node = result.register(this.expression())

                    defaultValue = node as Node?

                    skipNewLines(result)
                }

                argumentNames.add(ArgumentToken(name, defaultValue))

                while (this.currentToken.type == TokenType.COMMA) {
                    result.registerAdvancement()
                    this.advance()

                    skipNewLines(result)

                    if (this.currentToken.type != TokenType.IDENTIFIER) {
                        result.failure(
                            InvalidSyntaxError(
                                this.currentToken.start,
                                this.currentToken.end,
                                "Expected identifier"
                            )
                        )
                    }

                    val name = this.currentToken
                    var defaultValue: Node? = null

                    result.registerAdvancement()
                    this.advance()

                    skipNewLines(result)

                    if (optionals && this.currentToken.type != TokenType.EQUALS) {
                        result.failure(
                            InvalidSyntaxError(
                                this.currentToken.start,
                                this.currentToken.end,
                                "Argument '${name.value as String}' must be optional as previous arguments have been optional!"
                            )
                        )
                    }

                    if (this.currentToken.type == TokenType.EQUALS) {
                        optionals = true

                        result.registerAdvancement()
                        this.advance()

                        skipNewLines(result)

                        val node = result.register(this.expression())

                        defaultValue = node as Node?

                        skipNewLines(result)
                    }

                    argumentNames.add(ArgumentToken(name, defaultValue))

                    skipNewLines(result)
                }

                skipNewLines(result)

                if (this.currentToken.type != TokenType.RIGHT_PARENTHESES) {
                    result.failure(
                        InvalidSyntaxError(
                            this.currentToken.start,
                            this.currentToken.end,
                            "Expected ',' or ')', got $currentToken"
                        )
                    )
                }
            } else {
                skipNewLines(result)

                if (this.currentToken.type != TokenType.RIGHT_PARENTHESES) {
                    result.failure(
                        InvalidSyntaxError(
                            this.currentToken.start,
                            this.currentToken.end,
                            "Expected identifier or ')'"
                        )
                    )
                }
            }

            if (this.currentToken.type != TokenType.RIGHT_PARENTHESES) {
                result.failure(
                    InvalidSyntaxError(
                        this.currentToken.start,
                        this.currentToken.end,
                        "Container Definition: Expected ')', got $currentToken"
                    )
                )
            }

            result.registerAdvancement()
            this.advance()

            skipNewLines(result)
        }

        return argumentNames
    }

    private fun use(): ParseResult {
        val result = ParseResult()

        if (!this.currentToken.matches(TokenType.KEYWORD, Constants.get("import"))) {
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected '${Constants.get("import")}'"
                )
            )
        }

        result.registerAdvancement()
        this.advance()

        skipNewLines(result)

        if (this.currentToken.type != TokenType.STRING) {
            return result.failure(
                InvalidSyntaxError(
                    this.currentToken.start,
                    this.currentToken.end,
                    "Expected string"
                )
            )
        }

        val value = this.currentToken

        result.registerAdvancement()
        this.advance()

        val index = this.tokenIndex

        skipNewLines(result)

        var identifier = value

        if (this.currentToken.type == TokenType.ARROW) {
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

            identifier = this.currentToken

            result.registerAdvancement()
            this.advance()
        } else {
            this.reverse(this.tokenIndex - index)
        }

        return result.success(ImportNode(value, identifier, this.currentToken.start, this.currentToken.end))
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

            skipNewLines(result)

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

    open class BaseCase(val token: Node, val shouldReturnNull: Boolean) {
        override fun toString(): String {
            return "<Case: $token>"
        }
    }
    class Case(val node: Node, token: Node, shouldReturnNull: Boolean) : BaseCase(token, shouldReturnNull)
    class ArgumentToken(val token: Token, val defaultValue: Node?)

}