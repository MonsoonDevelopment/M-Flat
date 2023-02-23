package me.surge.api

import me.surge.lang.value.*
import me.surge.lang.value.number.NumberValue
import java.lang.IllegalStateException

object JavaCoercer {

    @JvmStatic
    fun coerce(obj: Value): Any? {
        return when (obj) {
            is NumberValue<*> -> coerceNumber(obj)
            is BooleanValue -> coerceBoolean(obj)
            is StringValue -> coerceString(obj)
            is ListValue -> coerceList(obj)
            is NullValue -> null
            else -> {
                throw IllegalStateException("obj wasn't value")
            }
        }
    }

    @JvmStatic
    fun coerceNumber(number: NumberValue<*>): Number {
        return number.value
    }

    @JvmStatic
    fun coerceBoolean(boolean: BooleanValue): Boolean {
        return boolean.value
    }

    @JvmStatic
    fun coerceString(string: StringValue): String {
        return string.value
    }

    @JvmStatic
    fun coerceList(list: ListValue): List<*> {
        val elements = arrayListOf<Any?>()

        list.elements.forEach {
            elements.add(coerce(it))
        }

        return elements
    }

}