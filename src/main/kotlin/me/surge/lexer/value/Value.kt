package me.surge.lexer.value

import me.surge.lexer.error.Error
import me.surge.lexer.error.context.Context
import me.surge.lexer.error.impl.RuntimeError
import me.surge.lexer.position.Position
import me.surge.lexer.symbol.SymbolTable
import me.surge.parse.RuntimeResult
import me.surge.util.Constants
import java.lang.IllegalStateException

open class Value(val identifier: String, var name: String = "any") {

    var start: Position? = null
    var end: Position? = null
    var context: Context? = null

    var symbols = SymbolTable()

    open fun addedTo(other: Value): Pair<Value?, Error?> = delegateToIllegal(other, "+")
    open fun subbedBy(other: Value): Pair<Value?, Error?> = delegateToIllegal(other, "-")
    open fun multedBy(other: Value): Pair<Value?, Error?> = delegateToIllegal(other, "*")
    open fun divedBy(other: Value): Pair<Value?, Error?> = delegateToIllegal(other, "/")
    open fun moduloedBy(other: Value): Pair<Value?, Error?> = delegateToIllegal(other, "%")
    open fun andedBy(other: Value): Pair<Value?, Error?> = delegateToIllegal(other, Constants.get("and"))
    open fun oredBy(other: Value): Pair<Value?, Error?> = delegateToIllegal(other, Constants.get("or"))
    open fun notted(): Pair<Value?, Error?> = delegateToIllegal(null, Constants.get("not"))

    open fun execute(args: List<Value> = arrayListOf(), context: Context): RuntimeResult = RuntimeResult().failure(this.delegateToIllegal(null, "execute").second)
    open fun execute(args: List<Value> = arrayListOf()): RuntimeResult = RuntimeResult().failure(this.delegateToIllegal(null, "execute").second)

    open fun compareEquality(other: Value): Pair<BooleanValue?, Error?> = this.delegateToIllegal(null, "==") as Pair<BooleanValue?, Error?>
    open fun compareInequality(other: Value): Pair<BooleanValue?, Error?> = this.delegateToIllegal(null, "!=") as Pair<BooleanValue?, Error?>
    open fun compareLessThan(other: Value): Pair<BooleanValue?, Error?> = this.delegateToIllegal(null, "<") as Pair<BooleanValue?, Error?>
    open fun compareGreaterThan(other: Value): Pair<BooleanValue?, Error?> = this.delegateToIllegal(null, ">") as Pair<BooleanValue?, Error?>
    open fun compareLessThanOrEqualTo(other: Value): Pair<BooleanValue?, Error?> = this.delegateToIllegal(null, "<=") as Pair<BooleanValue?, Error?>
    open fun compareGreaterThanOrEqualTo(other: Value): Pair<BooleanValue?, Error?> = this.delegateToIllegal(null, ">=") as Pair<BooleanValue?, Error?>

    open fun clone(): Value {
        return this
    }

    open fun isTrue(): Pair<Boolean, Error?> {
        return Pair(false, RuntimeError(
            this.start!!,
            this.end!!,
            "'${this.name}' cannot be interpreted as a boolean!",
            this.context!!
        ))
    }

    open fun stringValue(): String {
        throw IllegalStateException("`Value#stringValue` was not overridden!")
    }

    fun setPosition(start: Position?, end: Position?): Value {
        this.start = start
        this.end = end

        return this
    }

    fun setContext(context: Context?): Value {
        this.context = context

        return this
    }

    fun setSymbolTable(table: SymbolTable): Value {
        this.symbols = table

        return this
    }

    private fun delegateToIllegal(other: Value?, operation: String): Pair<Value?, RuntimeError> {
        val message = if (other != null) {
            "Illegal operation ('$operation') between values of types '${this.name}' and '${other.name}'"
        } else {
            "Illegal operation ('$operation') on value of type '${this.name}'"
        }

        return Pair(null, RuntimeError(
            this.start!!,
            this.end!!,
            message,
            this.context!!
        ))
    }

}