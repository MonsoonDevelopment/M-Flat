package me.surge.api

import me.surge.lexer.symbol.SymbolTable
import me.surge.lexer.value.ContainerInstanceValue
import me.surge.lexer.value.ContainerValue

object Coercer {

    @JvmStatic
    fun coerceObject(obj: Any): ContainerInstanceValue {
        val container = ContainerValue(obj.javaClass.simpleName, ArrayList())

        val table = SymbolTable()

        LoadHelper.loadClass(obj, table)

        return ContainerInstanceValue(container.name, table)
    }

}