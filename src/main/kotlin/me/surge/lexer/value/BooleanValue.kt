package me.surge.lexer.value

import me.surge.lexer.error.Error
import me.surge.util.binary
import me.surge.util.multiply

@ValueName("string")
class BooleanValue(name: String, val value: Boolean) : Value(name) {

    override fun compareEquality(other: Value): Pair<BooleanValue?, Error?> {
        return if (other is BooleanValue) {
            Pair(BooleanValue(name, this.value == other.value), null)
        } else {
            super.compareEquality(other)
        }
    }

    override fun compareInequality(other: Value): Pair<BooleanValue?, Error?> {
        return if (other is BooleanValue) {
            Pair(BooleanValue(name, this.value != other.value), null)
        } else {
            super.compareEquality(other)
        }
    }

    override fun andedBy(other: Value): Pair<Value?, Error?> {
        return if (other is BooleanValue) {
            Pair(BooleanValue(name, this.value && other.value), null)
        } else {
            super.andedBy(other)
        }
    }

    override fun oredBy(other: Value): Pair<Value?, Error?> {
        return if (other is BooleanValue) {
            Pair(BooleanValue(name, this.value || other.value), null)
        } else {
            super.andedBy(other)
        }
    }

    override fun notted(): Pair<Value?, Error?> {
        return Pair(BooleanValue(name, !this.value), null)
    }

    override fun isTrue(): Boolean {
        return this.value
    }

    override fun clone(): Value {
        return BooleanValue(name, this.value)
            .setPosition(this.start, this.end)
            .setContext(this.context)
    }

    override fun toString(): String {
        return "'${this.value}'"
    }

    override fun rawValue(): String {
        return this.value.toString()
    }

}