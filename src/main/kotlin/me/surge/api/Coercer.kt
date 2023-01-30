package me.surge.api

import me.surge.api.annotation.OverrideName
import me.surge.api.result.Failure
import me.surge.api.result.Success
import me.surge.lexer.error.context.Context
import me.surge.lexer.symbol.SymbolTable
import me.surge.lexer.value.*
import me.surge.lexer.value.function.BuiltInFunction
import me.surge.parse.RuntimeResult
import java.lang.reflect.Field
import java.lang.reflect.Method

object Coercer {

    @JvmStatic
    fun coerce(obj: Any?): Value {
        return when (obj) {
            is Value -> obj
            is Number -> coerceNumber(obj)
            is Boolean -> coerceBoolean(obj)
            is String -> coerceString(obj)
            is ArrayList<*> -> coerceList(obj)
            null -> NumberValue.NULL
            else -> {
                coerceObject(obj)
            }
        }
    }

    @JvmStatic
    fun coerceNumber(number: Number): NumberValue {
        return NumberValue("number", number)
    }

    @JvmStatic
    fun coerceBoolean(boolean: Boolean): BooleanValue {
        return BooleanValue("bool", boolean)
    }

    @JvmStatic
    fun coerceString(string: String): StringValue {
        return StringValue("str", string)
    }

    @JvmStatic
    fun coerceList(list: List<*>): ListValue {
        val arraylist = arrayListOf<Value>()

        list.forEach {
            arraylist.add(coerce(it))
        }

        return ListValue("list", arraylist)
    }

    @JvmStatic
    fun coerceMethod(instance: Any?, method: Method): BuiltInFunction {
        method.isAccessible = true

        val name = if (method.isAnnotationPresent(OverrideName::class.java)) {
            method.getAnnotation(OverrideName::class.java).name
        } else {
            method.name
        }

        val function = BuiltInFunction(
            name,

            { functionData ->
                val types = method.parameterTypes.filter { it != FunctionData::class.java }
                val converted = arrayListOf<Class<*>>()

                types.forEach {
                    converted.add(LoadHelper.getEquivalentValue(it))
                }

                val arguments = arrayListOf<Any>()

                if (method.parameters.any { it.type == FunctionData::class.java }) {
                    arguments.add(functionData)
                }

                functionData.arguments.forEachIndexed { index, value ->
                    arguments.add(LoadHelper.getEquivalentPrimitive(value, types[index]))
                }

                val result = method.invoke(instance, *arguments.toTypedArray())

                if (result is RuntimeResult) {
                    return@BuiltInFunction result
                }

                when (result) {
                    is Success -> {
                        RuntimeResult().success(result.value)
                    }

                    is Failure -> {
                        RuntimeResult().failure(result.error!!)
                    }

                    is Boolean -> {
                        RuntimeResult().success(BooleanValue(name, result))
                    }

                    is Number -> {
                        RuntimeResult().success(NumberValue(name, result))
                    }

                    is String -> {
                        RuntimeResult().success(StringValue(name, result))
                    }

                    else -> {
                        RuntimeResult().success(NumberValue.NULL)
                    }
                }
            },

            ArrayList(method.parameters.filter { it.type != FunctionData::class.java }.map { it.name }.toList())
        )

        return function
    }

    @JvmStatic
    fun coerceObject(obj: Any): ContainerInstanceValue {
        val container = ContainerValue(obj.javaClass.simpleName, ArrayList())

        val table = SymbolTable()

        LoadHelper.loadClass(obj, table)

        return ContainerInstanceValue(container.name, table, container)
    }

    @JvmStatic
    fun createContainer(instance: Any): ContainerValue {
        val table = SymbolTable()

        LoadHelper.loadClass(instance, table)

        val names = arrayListOf<String>()

        instance.javaClass.constructors[0].parameters.forEach {
            names += it.name
        }

        val container = ContainerValue(instance.javaClass.simpleName, names)

        container.implement = { _, context, _ ->
            val implContext = Context(instance.javaClass.simpleName, context)
            implContext.symbolTable = table

            container.context = implContext
        }

        return container
    }

}