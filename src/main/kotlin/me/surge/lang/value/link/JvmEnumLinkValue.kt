package me.surge.lang.value.link

import me.surge.api.LoadHelper
import me.surge.lang.symbol.SymbolTable
import me.surge.lang.util.Link
import me.surge.lang.value.number.NumberValue
import me.surge.lang.value.Value
import me.surge.lang.value.number.IntValue

class JvmEnumLinkValue(identifier: String, val enum: Class<Enum<*>>) : Value(identifier, "JVM ENUM"), Link {

    init {
        enum.enumConstants.forEach { member ->
            val instance = JvmClassInstanceValue(member.name, member)

            instance.symbols.set("ordinal", IntValue("ordinal", member.ordinal), SymbolTable.EntryData(immutable = true, declaration = true, this.start, this.end, this.context, forced = true))

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