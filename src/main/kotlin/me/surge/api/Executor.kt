package me.surge.api

import me.surge.library.Standard
import me.surge.interpreter.Interpreter
import me.surge.lexer.Lexer
import me.surge.lexer.error.Error
import me.surge.lexer.error.context.Context
import me.surge.lexer.error.impl.RuntimeError
import me.surge.lexer.node.Node
import me.surge.lexer.position.Position
import me.surge.lexer.symbol.SymbolTable
import me.surge.lexer.value.InstanceValue
import me.surge.lexer.value.Value
import me.surge.lexer.value.method.BaseMethodValue
import me.surge.library.BuiltIn
import me.surge.library.JVMLink
import me.surge.library.Maths
import me.surge.parse.Parser
import me.surge.parse.RuntimeResult
import java.io.File
import java.nio.charset.Charset

class Executor {

    private val globalSymbolTable = SymbolTable()
    private val silentContainers = hashMapOf<String, Value>()

    init {
        LoadHelper.loadClass(BuiltIn::class.java, globalSymbolTable)
        loadClass("std", Standard::class.java)
        loadSilentClass("maths", Maths::class.java)
        loadClass("jvmlink", JVMLink::class.java)
    }

    fun evaluate(file: String, text: String): Pair<Any?, Error?> {
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

        val interpreter = Interpreter(this)
        val context = Context("<program>")
        context.symbolTable = globalSymbolTable

        val result = interpreter.visit(ast.node!! as Node, context)

        return Pair(result.value, result.error)
    }

    fun use(file: String, text: String): Pair<Any?, Error?> {
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

        val interpreter = Interpreter(this)
        val context = Context(file)
        context.symbolTable = SymbolTable(globalSymbolTable)

        val result = interpreter.visit(ast.node!! as Node, context)

        globalSymbolTable.set(
            file,
            InstanceValue(file, context.symbolTable!!),
            SymbolTable.EntryData(
                immutable = true,
                declaration = true,
                null,
                null,
                context,
                forced = true
            )
        )

        return Pair(result.value, result.error)
    }

    fun useImplementation(name: String, start: Position, end: Position, context: Context): RuntimeResult {
        val result = RuntimeResult()

        if (name in silentContainers) {
            globalSymbolTable.set(name, silentContainers[name]!!, SymbolTable.EntryData(immutable = true, declaration = true, null, null, null, forced = true))
        } else {
            val file = File("$name.mfl")

            if (file.exists()) {
                val result = RuntimeResult()

                val import = this.use(name, file.readText(Charset.defaultCharset()))

                if (import.second != null) {
                    return result.failure(import.second!!)
                }

                return result.success(null)
            } else {
                return result.failure(
                    RuntimeError(
                        start,
                        end,
                        "Failed to import '$name': file not found!",
                        context
                    )
                )
            }
        }

        return result.success(null)
    }

    fun loadClass(identifier: String, any: Any): Executor {
        val symbolTable = SymbolTable()

        LoadHelper.loadClass(any, symbolTable)

        val value = InstanceValue(identifier, symbolTable)

        globalSymbolTable.set(identifier, value, SymbolTable.EntryData(immutable = true, declaration = true, null, null, null))

        return this
    }

    fun loadSilentClass(identifier: String, any: Any): Executor {
        val symbolTable = SymbolTable()

        LoadHelper.loadClass(any, symbolTable)

        silentContainers[identifier] = InstanceValue(identifier, symbolTable)

        return this
    }

    fun loadClassAsContainer(identifier: String, any: Any): Executor {
        LoadHelper.loadClassAsContainer(identifier, any, globalSymbolTable)
        return this
    }

    fun loadEnum(identifier: String, clazz: Class<*>): Executor {
        LoadHelper.loadEnum(identifier, clazz as Class<Enum<*>>, globalSymbolTable)
        return this
    }

    fun getFunction(identifier: String): BaseMethodValue? {
        return globalSymbolTable.get(identifier) as BaseMethodValue?
    }

    fun getValue(identifier: String): Value? {
        return globalSymbolTable.get(identifier) as Value?
    }

}