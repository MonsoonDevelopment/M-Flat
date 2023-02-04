package me.surge.library

import me.surge.api.JavaCoercer
import me.surge.lexer.symbol.SymbolTable
import me.surge.lexer.value.FunctionData
import me.surge.lexer.value.link.JvmClassLinkValue
import me.surge.lexer.value.ListValue
import me.surge.lexer.value.StringValue
import me.surge.lexer.value.method.BaseMethodValue

object JVMLink {

    fun classForName(functionData: FunctionData, path: StringValue, identifier: StringValue, arguments: ListValue) {
        val clazz = Class.forName(path.value)

        val elements = JavaCoercer.coerceList(arguments)

        val constructors = hashMapOf<Int, List<BaseMethodValue.Argument>>()

        val instance = clazz.getDeclaredConstructor(*elements.map { it?.javaClass }.toTypedArray()).newInstance(*elements.toTypedArray())

        instance.javaClass.constructors.forEach { constructor ->
            if (!constructor.isAccessible) {
                return@forEach
            }

            val list = arrayListOf<BaseMethodValue.Argument>()

            constructor.parameters.forEach { parameter ->
                list.add(BaseMethodValue.Argument(parameter.name))
            }

            constructors[list.size] = list
        }

        functionData.context?.parent?.parent?.symbolTable!!.set(identifier.value, JvmClassLinkValue(identifier.value, clazz, instance, constructors), SymbolTable.EntryData(immutable = true, declaration = true, functionData.start, functionData.end, functionData.context))
    }

}