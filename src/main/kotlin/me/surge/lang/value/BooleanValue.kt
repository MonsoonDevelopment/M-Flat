package me.surge.lang.value

import me.surge.api.LoadHelper
import me.surge.lang.error.Error

class BooleanValue(identifier: String, val value: Boolean) : Value(identifier, "boolean") {

    init {
        LoadHelper.loadClass(CompanionBuiltIns(this), this.symbols)
    }

    override fun andedBy(other: Value): Pair<Value?, Error?> = if (other is BooleanValue) Pair(BooleanValue(identifier, this.value && other.value), null) else super.andedBy(other)
    override fun oredBy(other: Value): Pair<Value?, Error?> = if (other is BooleanValue) Pair(BooleanValue(identifier, this.value || other.value), null) else super.oredBy(other)
    override fun notted(): Pair<Value?, Error?> = Pair(BooleanValue(identifier, !this.value), null)

    override fun compareEquality(other: Value): Pair<BooleanValue?, Error?> {
        if (other is NullValue) {
            return Pair(BooleanValue(identifier, false), null)
        }

        return if (other is BooleanValue) {
            Pair(BooleanValue(identifier, this.value == other.value), null)
        } else {
            super.compareEquality(other)
        }
    }

    override fun compareInequality(other: Value): Pair<BooleanValue?, Error?> {
        if (other is NullValue) {
            return Pair(BooleanValue(identifier, true), null)
        }

        return if (other is BooleanValue) {
            Pair(BooleanValue(identifier, this.value != other.value), null)
        } else {
            super.compareInequality(other)
        }
    }

    override fun isTrue(): Pair<Boolean, Error?> {
        return Pair(this.value, null)
    }

    override fun toString(): String {
        return stringValue()
    }

    override fun stringValue(): String {
        return super.overriddenString() ?: value.toString()
    }

    private class CompanionBuiltIns(val instance: BooleanValue) {

    }

}