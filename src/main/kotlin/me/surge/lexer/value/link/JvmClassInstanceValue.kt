package me.surge.lexer.value.link

import me.surge.api.LoadHelper
import me.surge.lexer.value.Value

class JvmClassInstanceValue<T : Any>(identifier: String, val instance: T) : Value(identifier, "JVM CLASS INSTANCE") {

    init {
        LoadHelper.loadClass(instance, this.symbols)
    }

    override fun toString(): String {
        return stringValue()
    }

    override fun stringValue(): String {
        return "<JVM INSTANCE [$identifier]>"
    }

}