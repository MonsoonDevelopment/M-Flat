package me.surge.api

import me.surge.api.flavour.Flavour
import me.surge.api.flavour.flavours.MFlatDefault
import me.surge.library.Standard
import me.surge.lang.interpreter.Interpreter
import me.surge.lang.error.Error
import me.surge.lang.error.context.Context
import me.surge.lang.error.impl.RuntimeError
import me.surge.lang.lexer.Lexer
import me.surge.lang.lexer.position.Position
import me.surge.lang.node.Node
import me.surge.lang.parse.Parser
import me.surge.lang.parse.RuntimeResult
import me.surge.lang.symbol.SymbolTable
import me.surge.lang.util.Constants
import me.surge.lang.util.Link
import me.surge.lang.value.InstanceValue
import me.surge.lang.value.Value
import me.surge.lang.value.method.BaseMethodValue
import me.surge.library.BuiltIn
import me.surge.library.JVMLink
import me.surge.library.Maths
import java.io.File
import java.nio.charset.Charset

class Executor(val flavour: Flavour = MFlatDefault) {

    val globalSymbolTable = SymbolTable()
    val silentContainers = hashMapOf<String, Value>()

    init {
        LoadHelper.loadClass(BuiltIn::class.java, globalSymbolTable)
        loadClass("std", Standard::class.java)
        loadSilentClass("maths", Maths::class.java)
        loadClass("jvmlink", JVMLink::class.java)
    }

    fun evaluate(file: String, text: String): Pair<Any?, Error?> {
        Constants.KEYWORDS = flavour.TOKENS
        val lexer = Lexer(file, text, this)
        val tokens = lexer.makeTokens()

        if (tokens.first.size == 1) {
            return Pair("NULL INPUT", null)
        }

        if (tokens.second != null) {
            return Pair(null, tokens.second)
        }

        val parser = Parser(tokens.first, this)
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

    fun use(file: String, text: String, identifier: String): Pair<Any?, Error?> {
        val lexer = Lexer(file, text, this)
        val tokens = lexer.makeTokens()

        if (tokens.first.size == 1) {
            return Pair("NULL INPUT", null)
        }

        if (tokens.second != null) {
            return Pair(null, tokens.second)
        }

        val parser = Parser(tokens.first, this)
        val ast = parser.parse()

        if (ast.node == null) {
            return Pair(null, ast.error)
        }

        val interpreter = Interpreter(this)
        val context = Context(file)
        context.symbolTable = SymbolTable(globalSymbolTable)

        val result = interpreter.visit(ast.node!! as Node, context)

        globalSymbolTable.set(
            identifier,
            InstanceValue(identifier, context.symbolTable!!, null),
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

    fun useImplementation(name: String, identifier: String, start: Position, end: Position, context: Context): RuntimeResult {
        val result = RuntimeResult()

        if (name in silentContainers) {
            globalSymbolTable.set(identifier, silentContainers[name]!!, SymbolTable.EntryData(immutable = true, declaration = true, null, null, null, forced = true))
        } else {
            val file = File("$name.mfl")

            if (file.exists()) {
                val result = RuntimeResult()

                val import = this.use(name, file.readText(Charset.defaultCharset()), identifier)

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

        val value = InstanceValue(identifier, symbolTable, null)

        globalSymbolTable.set(identifier, value, SymbolTable.EntryData(immutable = true, declaration = true, null, null, null))

        return this
    }

    fun loadSilentClass(identifier: String, any: Any): Executor {
        val symbolTable = SymbolTable()

        LoadHelper.loadClass(any, symbolTable)

        silentContainers[identifier] = InstanceValue(identifier, symbolTable, null)

        return this
    }

    fun loadClassAsContainer(identifier: String, any: Any, loadFieldsIntoSymbols: Boolean = false): Executor {
        LoadHelper.loadClassAsContainer(identifier, any, globalSymbolTable, loadFieldsIntoSymbols)
        return this
    }

    fun loadEnum(identifier: String, clazz: Class<*>): Executor {
        LoadHelper.loadEnum(identifier, clazz as Class<Enum<*>>, globalSymbolTable)
        return this
    }

    fun reset() {
        globalSymbolTable.symbols.removeIf { it.value !is Link }
    }

    fun getFunction(identifier: String): BaseMethodValue? {
        return globalSymbolTable.get(identifier) as BaseMethodValue?
    }

    fun getValue(identifier: String): Value? {
        return globalSymbolTable.get(identifier)
    }

}