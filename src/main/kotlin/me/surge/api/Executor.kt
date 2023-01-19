package me.surge.api

import me.surge.BuiltIns
import me.surge.interpreter.Interpreter
import me.surge.lexer.Lexer
import me.surge.lexer.error.Error
import me.surge.lexer.error.context.Context
import me.surge.lexer.node.ListNode
import me.surge.lexer.node.Node
import me.surge.lexer.symbol.SymbolTable
import me.surge.lexer.value.Value
import me.surge.lexer.value.function.BaseFunctionValue
import me.surge.lexer.value.function.FunctionValue
import me.surge.parse.Parser

class Executor {

    private val globalSymbolTable = SymbolTable()

    init {
        loadClass(BuiltIns::class.java)
    }

    fun run(file: String, text: String): Pair<Any?, Error?> {
        val lexer = Lexer(file, text)
        val tokens = lexer.makeTokens()

        if (tokens.first.size == 1) {
            return Pair("NULL INPUT", null)
        }

        if (tokens.second != null) {
            return Pair(null, tokens.second)
        }

        val parser = Parser(tokens.first)
        val ast = parser.parse()

        if (ast.node == null) {
            return Pair(null, ast.error)
        }

        val interpreter = Interpreter()
        val context = Context("<program>")
        context.symbolTable = globalSymbolTable

        val result = interpreter.visit(ast.node!! as Node, context)

        return Pair(result.value, result.error)
    }

    fun loadClass(any: Any): Executor {
        LoadHelper.loadClass(any, globalSymbolTable)

        return this
    }

    fun loadClass(clazz: Class<*>): Executor {
        LoadHelper.loadClass(clazz, globalSymbolTable)

        return this
    }

    fun getFunction(name: String): BaseFunctionValue? {
        return globalSymbolTable.get(name) as BaseFunctionValue?
    }

    fun getValue(name: String): Value {
        return globalSymbolTable.get(name) as Value
    }

}