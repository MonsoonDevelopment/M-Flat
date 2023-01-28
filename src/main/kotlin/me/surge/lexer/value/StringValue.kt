package me.surge.lexer.value

import me.surge.lexer.error.Error
import me.surge.util.binary
import me.surge.util.multiply

@ValueName("string")
class StringValue(name: String, val value: String) : Value(name) {

    override fun addedTo(other: Value): Pair<Value?, Error?> {
        return if (other is StringValue || other is NumberValue) {
            Pair(StringValue(name, this.value + other.rawValue()).setContext(this.context), null)
        } else {
            super.addedTo(other)
        }
    }

    override fun multedBy(other: Value): Pair<Value?, Error?> {
        return if (other is NumberValue) {
            Pair(StringValue(name, this.value.multiply(other.value.toInt())).setContext(this.context), null)
        } else {
            super.addedTo(other)
        }
    }

    override fun compareEquality(other: Value): Pair<BooleanValue?, Error?> {
        return if (other is StringValue) {
            Pair(BooleanValue(name, this.rawValue() == other.rawValue()), null)
        } else {
            super.compareEquality(other)
        }
    }

    override fun isTrue(): Boolean {
        return this.value.isNotEmpty()
    }

    override fun clone(): Value {
        return StringValue(name, this.value)
            .setPosition(this.start, this.end)
            .setContext(this.context)
    }

    override fun toString(): String {
        return "'${this.value}'"
    }

    override fun rawValue(): String {
        return this.value
    }

}