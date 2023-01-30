package me.surge.lexer.value

import me.surge.util.Constants
import me.surge.lexer.error.Error
import me.surge.lexer.error.context.Context
import me.surge.lexer.error.impl.RuntimeError
import me.surge.lexer.position.Position
import me.surge.parse.RuntimeResult
import java.lang.IllegalStateException

@ValueName("any")
open class Value(var name: String) {

    var start: Position? = null
    var end: Position? = null
    var context: Context? = null

    val rawName = this::class.java.getAnnotation(ValueName::class.java).name

    fun setPosition(start: Position? = null, end: Position? = null): Value {
        this.start = start
        this.end = end

        return this
    }

    fun setContext(context: Context? = null): Value {
        this.context = context

        return this
    }

    open fun addedTo(other: Value): Pair<Value?, Error?> = Pair(null, illegalOperation("+", other))
    open fun subbedBy(other: Value): Pair<Value?, Error?> = Pair(null, illegalOperation("-", other))
    open fun multedBy(other: Value): Pair<Value?, Error?> = Pair(null, illegalOperation("*", other))
    open fun divedBy(other: Value): Pair<Value?, Error?> = Pair(null, illegalOperation("/", other))
    open fun powedBy(other: Value): Pair<Value?, Error?> = Pair(null, illegalOperation("^", other))
    open fun moduloedBy(other: Value): Pair<Value?, Error?> = Pair(null, illegalOperation("%", other))
    open fun andedBy(other: Value): Pair<Value?, Error?> = Pair(null, illegalOperation(Constants.get("and"), other))
    open fun oredBy(other: Value): Pair<Value?, Error?> = Pair(null, illegalOperation(Constants.get("ot"), other))
    open fun notted(): Pair<Value?, Error?> = Pair(null, illegalOperation(Constants.get("not")))

    open fun execute(args: ArrayList<Value> = arrayListOf(), context: Context): RuntimeResult = RuntimeResult().failure(this.illegalOperation("execute"))
    open fun execute(args: ArrayList<Value> = arrayListOf()): RuntimeResult = RuntimeResult().failure(this.illegalOperation("execute"))

    open fun compareEquality(other: Value): Pair<BooleanValue?, Error?> = Pair(null, illegalOperation("==", other))
    open fun compareInequality(other: Value): Pair<BooleanValue?, Error?> = Pair(null, illegalOperation("!=", other))
    open fun compareLessThan(other: Value): Pair<BooleanValue?, Error?> = Pair(null, illegalOperation("<", other))
    open fun compareGreaterThan(other: Value): Pair<BooleanValue?, Error?> = Pair(null, illegalOperation(">", other))
    open fun compareLessThanOrEqualTo(other: Value): Pair<BooleanValue?, Error?> = Pair(null, illegalOperation("<=", other))
    open fun compareGreaterThanOrEqualTo(other: Value): Pair<BooleanValue?, Error?> = Pair(null, illegalOperation(">=", other))

    fun illegalOperation(operation: String, other: Any? = null): RuntimeError {
        var other = other

        if (other == null) {
            other = this
        }

        return RuntimeError(
            this.start!!,
            this.end!!,
            "Illegal Operation: '$operation'",
            this.context!!
        )
    }

    open fun clone(): Value = this

    open fun isTrue(): Boolean {
        return false
    }

    open fun rawValue(): String {
        throw IllegalStateException("Raw value not found")
    }

}