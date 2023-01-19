package me.surge.lexer.value

import me.surge.lexer.error.Error
import me.surge.lexer.error.impl.RuntimeError
import me.surge.lexer.node.Node
import me.surge.util.multiply
import java.util.Collections

@ValueName("list")
class ListValue(name: String, val elements: ArrayList<Value>) : Value(name) {

    override fun addedTo(other: Value): Pair<Value?, Error?> {
        val new = this.clone() as ListValue
        new.elements.add(other)

        return Pair(new, null)
    }

    override fun subbedBy(other: Value): Pair<Value?, Error?> {
        if (other is NumberValue) {
            val new = this.clone() as ListValue

            return try {
                new.elements.removeAt(other.value.toInt())
                Pair(new, null)
            } catch (exception: Exception) {
                Pair(null, RuntimeError(
                    other.start!!,
                    other.end!!,
                    "Index out of bounds: ${other.value.toInt()}",
                    this.context!!
                ))
            }
        } else {
            return super.subbedBy(other)
        }
    }

    override fun multedBy(other: Value): Pair<Value?, Error?> {
        return if (other is ListValue) {
            val new = this.clone() as ListValue
            new.elements.addAll(other.elements)
            return Pair(new, null)
        } else {
            super.multedBy(other)
        }
    }

    override fun divedBy(other: Value): Pair<Value?, Error?> {
        if (other is NumberValue) {
            return try {
                return Pair(this.elements[other.value.toInt()], null)
            } catch (exception: Exception) {
                return Pair(null, RuntimeError(
                    other.start!!,
                    other.end!!,
                    "Index out of bounds: ${other.value.toInt()}",
                    this.context!!
                ))
            }
        } else {
            return super.divedBy(other)
        }
    }

    override fun isTrue(): Boolean {
        return this.elements.isNotEmpty()
    }

    override fun clone(): Value {
        return ListValue(name, ArrayList(this.elements))
            .setPosition(this.start, this.end)
            .setContext(this.context)
    }

    override fun toString(): String {
        return "'${this.elements}'"
    }

    override fun rawValue(): String {
        return this.elements.toString()
    }

}