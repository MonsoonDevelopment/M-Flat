package me.surge.lang.value.number

import me.surge.api.LoadHelper
import me.surge.lang.error.Error
import me.surge.lang.error.impl.RuntimeError
import me.surge.lang.value.BooleanValue
import me.surge.lang.value.NullValue
import me.surge.lang.value.StringValue
import me.surge.lang.value.Value

open class NumberValue<T : Number>(identifier: String, name: String, val value: T) : Value(identifier, name) {

    init {
        LoadHelper.loadClass(CompanionBuiltIns(this), this.symbols)
    }

    override fun addedTo(other: Value): Pair<Value?, Error?> {
        if (other is NumberValue<*>) {
            val float = this.value is Float || other.value is Float

            val value = if (float) {
                val number = this.value.toFloat() + other.value.toFloat()
                FloatValue(number.toString(), number)
            } else {
                val number = this.value.toInt() + other.value.toInt()
                IntValue(number.toString(), number)
            }

            return Pair(value, null)
        }

        return Pair(StringValue(identifier, this.value.toString() + other.stringValue()), null)
    }

    override fun subbedBy(other: Value): Pair<Value?, Error?> {
        if (other is NumberValue<*>) {
            val float = this.value is Float || other.value is Float

            val value = if (float) {
                val number = this.value.toFloat() - other.value.toFloat()
                FloatValue(number.toString(), number)
            } else {
                val number = this.value.toInt() - other.value.toInt()
                IntValue(number.toString(), number)
            }

            return Pair(value, null)
        }

        return super.subbedBy(other)
    }

    override fun multedBy(other: Value): Pair<Value?, Error?> {
        if (other is NumberValue<*>) {
            val float = this.value is Float || other.value is Float

            val value = if (float) {
                val number = this.value.toFloat() * other.value.toFloat()
                FloatValue(number.toString(), number)
            } else {
                val number = this.value.toInt() * other.value.toInt()
                IntValue(number.toString(), number)
            }

            return Pair(value, null)
        }

        return super.multedBy(other)
    }

    override fun divedBy(other: Value): Pair<Value?, Error?> {
        if (other is NumberValue<*>) {
            if (other.value.toFloat() == 0f) {
                return Pair(null, RuntimeError(
                    other.start!!,
                    other.end!!,
                    "Division by zero",
                    this.context!!
                ))
            }

            val float = this.value is Float || other.value is Float

            val value = if (float) {
                val number = this.value.toFloat() / other.value.toFloat()
                FloatValue(number.toString(), number)
            } else {
                val number = this.value.toInt() / other.value.toInt()
                IntValue(number.toString(), number)
            }

            return Pair(value, null)
        }

        return super.divedBy(other)
    }

    override fun moduloedBy(other: Value): Pair<Value?, Error?> {
        if (other is NumberValue<*>) {
            val float = this.value is Float || other.value is Float

            val value = if (float) {
                val number = this.value.toFloat() % other.value.toFloat()
                FloatValue(number.toString(), number)
            } else {
                val number = this.value.toInt() % other.value.toInt()
                IntValue(number.toString(), number)
            }

            return Pair(value, null)
        }

        return super.moduloedBy(other)
    }

    override fun compareEquality(other: Value): Pair<BooleanValue?, Error?> {
        if (other is NullValue) {
            return Pair(BooleanValue(identifier, false), null)
        }

        if (other is NumberValue<*>) {
            return Pair(BooleanValue(identifier, if (this.value is Float || other.value is Float) this.value.toFloat() == other.value.toFloat() else this.value.toInt() == other.value.toInt()), null)
        }

        return super.compareEquality(other)
    }

    override fun compareInequality(other: Value): Pair<BooleanValue?, Error?> {
        if (other is NullValue) {
            return Pair(BooleanValue(identifier, true), null)
        }

        if (other is NumberValue<*>) {
            return Pair(BooleanValue(identifier, if (this.value is Float || other.value is Float) this.value.toFloat() != other.value.toFloat() else this.value.toInt() != other.value.toInt()), null)
        }

        return super.compareInequality(other)
    }

    override fun compareLessThan(other: Value): Pair<BooleanValue?, Error?> {
        if (other is NumberValue<*>) {
            return Pair(BooleanValue(identifier, if (this.value is Float || other.value is Float) this.value.toFloat() < other.value.toFloat() else this.value.toInt() < other.value.toInt()), null)
        }

        return super.compareLessThan(other)
    }

    override fun compareGreaterThan(other: Value): Pair<BooleanValue?, Error?> {
        if (other is NumberValue<*>) {
            return Pair(BooleanValue(identifier, if (this.value is Float || other.value is Float) this.value.toFloat() > other.value.toFloat() else this.value.toInt() > other.value.toInt()), null)
        }

        return super.compareGreaterThan(other)
    }

    override fun compareLessThanOrEqualTo(other: Value): Pair<BooleanValue?, Error?> {
        if (other is NumberValue<*>) {
            return Pair(BooleanValue(identifier, if (this.value is Float || other.value is Float) this.value.toFloat() <= other.value.toFloat() else this.value.toInt() <= other.value.toInt()), null)
        }

        return super.compareLessThanOrEqualTo(other)
    }

    override fun compareGreaterThanOrEqualTo(other: Value): Pair<BooleanValue?, Error?> {
        if (other is NumberValue<*>) {
            return Pair(BooleanValue(identifier, if (this.value is Float || other.value is Float) this.value.toFloat() >= other.value.toFloat() else this.value.toInt() >= other.value.toInt()), null)
        }

        return super.compareGreaterThanOrEqualTo(other)
    }

    override fun clone(): NumberValue<*> {
        return NumberValue(identifier, this.name, this.value)
            .setPosition(this.start, this.end)
            .setContext(this.context) as NumberValue<*>
    }

    override fun isOfType(type: String): Boolean {
        return super.isOfType(type) || type == "number"
    }

    override fun toString(): String {
        return stringValue()
    }

    override fun stringValue(): String {
        return value.toString()
    }

    private class CompanionBuiltIns(val instance: NumberValue<*>) {

    }

}