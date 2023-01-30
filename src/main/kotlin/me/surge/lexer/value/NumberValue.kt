package me.surge.lexer.value

import me.surge.lexer.error.Error
import me.surge.lexer.error.impl.RuntimeError
import me.surge.util.binary
import me.surge.util.boolean
import kotlin.math.pow

@ValueName("number")
class NumberValue(name: String, val value: Number) : Value(name) {

    override fun addedTo(other: Value): Pair<Value?, Error?> {
        return if (other is NumberValue) {
            Pair(NumberValue(name, if (this.value is Float) this.value.toFloat() + other.value.toFloat() else this.value.toInt() + other.value.toInt()).setContext(this.context), null)
        } else {
            Pair(null, illegalOperation("+", other))
        }
    }

    override fun subbedBy(other: Value): Pair<Value?, Error?> {
        return if (other is NumberValue) {
            Pair(NumberValue(name, if (this.value is Float) this.value.toFloat() - other.value.toFloat() else this.value.toInt() - other.value.toInt()).setContext(this.context), null)
        } else {
            Pair(null, illegalOperation("-", other))
        }
    }

    override fun multedBy(other: Value): Pair<Value?, Error?> {
        return if (other is NumberValue) {
            Pair(NumberValue(name, if (this.value is Float) this.value.toFloat() * other.value.toFloat() else this.value.toInt() * other.value.toInt()).setContext(this.context), null)
        } else {
            Pair(null, illegalOperation("*", other))
        }
    }

    override fun divedBy(other: Value): Pair<Value?, Error?> {
        return if (other is NumberValue) {
            if (other.value == 0) {
                return Pair(null, RuntimeError(
                    other.start!!,
                    other.end!!,
                    "Division by zero",
                    this.context!!
                ))
            }

            Pair(NumberValue(name, if (this.value is Float) this.value.toFloat() / other.value.toFloat() else this.value.toInt() / other.value.toInt()).setContext(this.context), null)
        } else {
            Pair(null, illegalOperation("/", other))
        }
    }

    override fun powedBy(other: Value): Pair<Value?, Error?> {
        return if (other is NumberValue) {
            Pair(NumberValue(name, if (this.value is Float) this.value.toFloat().pow(other.value.toFloat()) else this.value.toFloat().pow(other.value.toFloat()).toInt()).setContext(this.context), null)
        } else {
            Pair(null, illegalOperation("^", other))
        }
    }

    override fun compareEquality(other: Value): Pair<BooleanValue?, Error?> {
        return if (other is NumberValue) {
            Pair(BooleanValue(name, if (this.value is Float) this.value.toFloat() == other.value.toFloat() else this.value.toInt() == other.value.toInt()).setContext(this.context) as BooleanValue, null)
        } else {
            Pair(null, illegalOperation("==", other))
        }
    }

    override fun compareInequality(other: Value): Pair<BooleanValue?, Error?> {
        return if (other is NumberValue) {
            Pair(BooleanValue(name, if (this.value is Float) this.value.toFloat() != other.value.toFloat() else this.value.toInt() != other.value.toInt()).setContext(this.context) as BooleanValue, null)
        } else {
            Pair(null, illegalOperation("!=", other))
        }
    }

    override fun compareLessThan(other: Value): Pair<BooleanValue?, Error?> {
        return if (other is NumberValue) {
            Pair(BooleanValue(name, if (this.value is Float) this.value.toFloat() < other.value.toFloat() else this.value.toInt() < other.value.toInt()).setContext(this.context) as BooleanValue, null)
        } else {
            Pair(null, illegalOperation("<", other))
        }
    }

    override fun compareGreaterThan(other: Value): Pair<BooleanValue?, Error?> {
        return if (other is NumberValue) {
            Pair(BooleanValue(name, if (this.value is Float) this.value.toFloat() > other.value.toFloat() else this.value.toInt() > other.value.toInt()).setContext(this.context) as BooleanValue, null)
        } else {
            Pair(null, illegalOperation(">", other))
        }
    }

    override fun compareLessThanOrEqualTo(other: Value): Pair<BooleanValue?, Error?> {
        return if (other is NumberValue) {
            Pair(BooleanValue(name, if (this.value is Float) this.value.toFloat() <= other.value.toFloat() else this.value.toInt() <= other.value.toInt()).setContext(this.context) as BooleanValue, null)
        } else {
            Pair(null, illegalOperation("<=", other))
        }
    }

    override fun compareGreaterThanOrEqualTo(other: Value): Pair<BooleanValue?, Error?> {
        return if (other is NumberValue) {
            Pair(BooleanValue(name, if (this.value is Float) this.value.toFloat() >= other.value.toFloat() else this.value.toInt() >= other.value.toInt()).setContext(this.context) as BooleanValue, null)
        } else {
            Pair(null, illegalOperation(">=", other))
        }
    }

    override fun clone(): NumberValue {
        return NumberValue(name, this.value)
            .setPosition(this.start, this.end)
            .setContext(this.context) as NumberValue
    }

    override fun isTrue(): Boolean {
        return this.value != 0
    }

    override fun toString(): String {
        return this.value.toString()
    }

    override fun rawValue(): String {
        return this.value.toString()
    }

}