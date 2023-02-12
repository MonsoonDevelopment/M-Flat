package me.surge.lang.value

import me.surge.api.LoadHelper
import me.surge.lang.error.Error

class ListValue(identifier: String, val elements: MutableList<Value>) : Value(identifier, "list") {

    init {
        LoadHelper.loadClass(CompanionBuiltIns(this), this.symbols)
    }

    override fun compareEquality(other: Value): Pair<BooleanValue?, Error?> {
        if (other is NullValue) {
            return Pair(BooleanValue(identifier, false), null)
        }

        return if (other is ListValue) {
            Pair(BooleanValue(identifier, this == other), null)
        } else {
            super.compareEquality(other)
        }
    }

    override fun compareInequality(other: Value): Pair<BooleanValue?, Error?> {
        if (other is NullValue) {
            return Pair(BooleanValue(identifier, true), null)
        }

        return if (other is ListValue) {
            Pair(BooleanValue(identifier, this != other), null)
        } else {
            super.compareInequality(other)
        }
    }

    override fun clone(): ListValue {
        return ListValue(name, ArrayList(elements))
            .setPosition(this.start, this.end)
            .setContext(this.context) as ListValue
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ListValue) {
            return false
        }

        return this.elements == other.elements
    }

    override fun toString(): String {
        return stringValue()
    }

    override fun stringValue(): String {
        return this.elements.toString()
    }

    private class CompanionBuiltIns(val instance: ListValue) {

        fun add(value: Value) {
            instance.elements.add(value)
        }

        fun remove(index: NumberValue) {
            instance.elements.removeAt(index.value.toInt())
        }

    }

}