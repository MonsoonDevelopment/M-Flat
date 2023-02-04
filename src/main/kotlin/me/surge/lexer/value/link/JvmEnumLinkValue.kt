package me.surge.lexer.value.link

import me.surge.api.LoadHelper
import me.surge.lexer.symbol.SymbolTable
import me.surge.lexer.value.NumberValue
import me.surge.lexer.value.Value

class JvmEnumLinkValue(identifier: String, val enum: Class<Enum<*>>) : Value(identifier, "JVM ENUM") {

    init {
        enum.enumConstants.forEach { member ->
            val instance = JvmClassInstanceValue(member.name, member)

            instance.symbols.set("ordinal", NumberValue("ordinal", member.ordinal), SymbolTable.EntryData(immutable = true, declaration = true, this.start, this.end, this.context, forced = true))

            LoadHelper.loadClass(member, instance.symbols)

            this.symbols.set(member.name, instance, SymbolTable.EntryData(immutable = true, declaration = true, this.start, this.end, this.context, forced = true))
        }
    }

    override fun toString(): String {
        return stringValue()
    }

    override fun stringValue(): String {
        return "<JVM ENUM LINK [$identifier]>"
    }

}