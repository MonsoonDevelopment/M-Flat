package me.surge.lexer.value

import me.surge.api.LoadHelper
import me.surge.lexer.error.Error
import me.surge.lexer.error.impl.RuntimeError

class NumberValue(identifier: String, val value: Number) : Value(identifier, "number") {

    init {
        LoadHelper.loadClass(CompanionBuiltIns(this), this.symbols)
    }

    override fun addedTo(other: Value): Pair<Value?, Error?> {
        if (other is NumberValue) {
            return Pair(NumberValue(identifier, if (this.value is Float || other.value is Float) this.value.toFloat() + other.value.toFloat() else this.value.toInt() + other.value.toInt()), null)
        }

        return super.addedTo(other)
    }

    override fun subbedBy(other: Value): Pair<Value?, Error?> {
        if (other is NumberValue) {
            return Pair(NumberValue(identifier, if (this.value is Float || other.value is Float) this.value.toFloat() - other.value.toFloat() else this.value.toInt() - other.value.toInt()), null)
        }

        return super.subbedBy(other)
    }

    override fun multedBy(other: Value): Pair<Value?, Error?> {
        if (other is NumberValue) {
            return Pair(NumberValue(identifier, if (this.value is Float || other.value is Float) this.value.toFloat() * other.value.toFloat() else this.value.toInt() * other.value.toInt()), null)
        }

        return super.multedBy(other)
    }

    override fun divedBy(other: Value): Pair<Value?, Error?> {
        if (other is NumberValue) {
            if (other.value.toFloat() == 0f) {
                return Pair(null, RuntimeError(
                    other.start!!,
                    other.end!!,
                    "Division by zero",
                    this.context!!
                ))
            }

            return Pair(NumberValue(identifier, if (this.value is Float || other.value is Float) this.value.toFloat() / other.value.toFloat() else this.value.toInt() / other.value.toInt()), null)
        }

        return super.divedBy(other)
    }

    override fun moduloedBy(other: Value): Pair<Value?, Error?> {
        if (other is NumberValue) {
            return Pair(NumberValue(identifier, if (this.value is Float || other.value is Float) this.value.toFloat() % other.value.toFloat() else this.value.toInt() % other.value.toInt()), null)
        }

        return super.moduloedBy(other)
    }

    override fun compareEquality(other: Value): Pair<BooleanValue?, Error?> {
        if (other is NullValue) {
            return Pair(BooleanValue(identifier, false), null)
        }

        if (other is NumberValue) {
            return Pair(BooleanValue(identifier, if (this.value is Float || other.value is Float) this.value.toFloat() == other.value.toFloat() else this.value.toInt() == other.value.toInt()), null)
        }

        return super.compareEquality(other)
    }

    override fun compareInequality(other: Value): Pair<BooleanValue?, Error?> {
        if (other is NullValue) {
            return Pair(BooleanValue(identifier, true), null)
        }

        if (other is NumberValue) {
            return Pair(BooleanValue(identifier, if (this.value is Float || other.value is Float) this.value.toFloat() != other.value.toFloat() else this.value.toInt() != other.value.toInt()), null)
        }

        return super.compareInequality(other)
    }

    override fun compareLessThan(other: Value): Pair<BooleanValue?, Error?> {
        if (other is NumberValue) {
            return Pair(BooleanValue(identifier, if (this.value is Float || other.value is Float) this.value.toFloat() < other.value.toFloat() else this.value.toInt() < other.value.toInt()), null)
        }

        return super.compareLessThan(other)
    }

    override fun compareGreaterThan(other: Value): Pair<BooleanValue?, Error?> {
        if (other is NumberValue) {
            return Pair(BooleanValue(identifier, if (this.value is Float || other.value is Float) this.value.toFloat() > other.value.toFloat() else this.value.toInt() > other.value.toInt()), null)
        }

        return super.compareGreaterThan(other)
    }

    override fun compareLessThanOrEqualTo(other: Value): Pair<BooleanValue?, Error?> {
        if (other is NumberValue) {
            return Pair(BooleanValue(identifier, if (this.value is Float || other.value is Float) this.value.toFloat() <= other.value.toFloat() else this.value.toInt() <= other.value.toInt()), null)
        }

        return super.compareLessThanOrEqualTo(other)
    }

    override fun compareGreaterThanOrEqualTo(other: Value): Pair<BooleanValue?, Error?> {
        if (other is NumberValue) {
            return Pair(BooleanValue(identifier, if (this.value is Float || other.value is Float) this.value.toFloat() >= other.value.toFloat() else this.value.toInt() >= other.value.toInt()), null)
        }

        return super.compareGreaterThanOrEqualTo(other)
    }

    override fun clone(): NumberValue {
        return NumberValue(identifier, this.value)
            .setPosition(this.start, this.end)
            .setContext(this.context) as NumberValue
    }

    override fun toString(): String {
        return stringValue()
    }

    override fun stringValue(): String {
        return value.toString()
    }

    private class CompanionBuiltIns(val instance: NumberValue) {

    }

}