package me.surge.lang.value

import me.surge.api.LoadHelper
import me.surge.api.result.Result
import me.surge.api.result.Success
import me.surge.lang.error.Error
import me.surge.lang.util.multiply

class StringValue(identifier: String, var value: String) : Value(identifier, "string") {

    init {
        LoadHelper.loadClass(CompanionBuiltIns(this), this.symbols)
    }

    override fun addedTo(other: Value): Pair<Value?, Error?> {
        return Pair(StringValue(this.identifier, this.value + other.stringValue()), null)
    }

    override fun multedBy(other: Value): Pair<Value?, Error?> {
        return if (other is NumberValue) {
            Pair(StringValue(name, this.value.multiply(other.value.toInt())).setContext(this.context), null)
        } else {
            super.addedTo(other)
        }
    }

    override fun compareEquality(other: Value): Pair<BooleanValue?, Error?> {
        if (other is NullValue) {
            return Pair(BooleanValue(identifier, false), null)
        }

        return if (other is StringValue) {
            Pair(BooleanValue(identifier, this.value == other.value), null)
        } else {
            super.compareEquality(other)
        }
    }
    override fun compareInequality(other: Value): Pair<BooleanValue?, Error?> {
        if (other is NullValue) {
            return Pair(BooleanValue(identifier, true), null)
        }

        return if (other is StringValue) {
            Pair(BooleanValue(identifier, this.value != other.value), null)
        } else {
            super.compareInequality(other)
        }
    }

    override fun toString(): String {
        return "\"${stringValue()}\""
    }

    override fun stringValue(): String {
        return value
    }

    private class CompanionBuiltIns(val instance: StringValue) {

        fun upper(): Result {
            return Success(StringValue(instance.identifier, instance.stringValue().uppercase()))
        }

        fun lower(): Result {
            return Success(StringValue(instance.identifier, instance.stringValue().lowercase()))
        }

        fun equalsIgnoreCase(other: StringValue): Result {
            return Success(BooleanValue(instance.identifier, instance.value.equals(other.value, true)))
        }

    }

}