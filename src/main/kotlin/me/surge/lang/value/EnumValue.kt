package me.surge.lang.value

import me.surge.lang.symbol.SymbolTable

class EnumValue(identifier: String, members: LinkedHashMap<String, Value>) : Value(identifier, "enum") {

    init {
        members.forEach { (name, value) ->
            this.symbols.set(name, value, SymbolTable.EntryData(immutable = true, declaration = true, this.start, this.end, this.context, forced = true))
        }
    }

    override fun clone(): Value {
        return this
    }

    override fun toString(): String {
        return stringValue()
    }

    override fun stringValue(): String {
        return super.overriddenString() ?: "<enum $identifier>"
    }

}