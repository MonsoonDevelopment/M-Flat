package me.surge.api

import me.surge.api.annotation.OverrideName
import me.surge.api.result.Failure
import me.surge.api.result.Success
import me.surge.lang.error.context.Context
import me.surge.lang.parse.RuntimeResult
import me.surge.lang.symbol.SymbolTable
import me.surge.lang.value.*
import me.surge.lang.value.link.*
import me.surge.lang.value.method.*
import me.surge.lang.value.number.FloatValue
import me.surge.lang.value.number.IntValue
import me.surge.lang.value.number.NumberValue
import java.lang.reflect.Method

object Coercer {

    @JvmStatic
    fun coerce(obj: Any?): Value {
        return when (obj) {
            is Value -> obj
            is Number -> coerceNumber(obj)
            is Boolean -> coerceBoolean(obj)
            is String -> coerceString(obj)
            is List<*> -> coerceList(obj)
            null -> NullValue()
            else -> {
                coerceObject(obj)
            }
        }
    }

    @JvmStatic
    fun coerceNumber(number: Number): NumberValue<*> {
        return if (number.toString().contains('.')) {
            FloatValue("number", number.toFloat())
        } else {
            IntValue("number", number.toInt())
        }
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
    fun coerceMethod(instance: Any?, method: Method): JvmMethodLink {
        method.isAccessible = true

        val name = if (method.isAnnotationPresent(OverrideName::class.java)) {
            method.getAnnotation(OverrideName::class.java).name
        } else {
            method.name
        }

        val function = JvmMethodLink(
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
                    return@JvmMethodLink result
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
                        RuntimeResult().success(if (result.toString().contains('.')) {
                            FloatValue("number", result.toFloat())
                        } else {
                            IntValue("number", result.toInt())
                        })
                    }

                    is String -> {
                        RuntimeResult().success(StringValue(name, result))
                    }

                    else -> {
                        RuntimeResult().success(NullValue())
                    }
                }
            },

            ArrayList(method.parameters.filter { it.type != FunctionData::class.java }.map { BaseMethodValue.Argument(it.name) }.toList())
        )

        return function
    }

    @JvmStatic
    fun coerceObject(obj: Any): Value {
        val container = ContainerValue(obj.javaClass.simpleName, hashMapOf())

        val table = SymbolTable()

        LoadHelper.loadClass(obj, table)

        return InstanceValue(container.identifier, table, container)
    }

    @JvmStatic
    fun createContainer(instance: Any): ContainerValue {
        val table = SymbolTable()

        LoadHelper.loadClass(instance, table)

        val names = arrayListOf<BaseMethodValue.Argument>()

        instance.javaClass.constructors[0].parameters.forEach {
            names += BaseMethodValue.Argument(it.name)
        }

        val container = ContainerValue(instance.javaClass.simpleName, hashMapOf(Pair(names.size, names)))

        container.implement = { _, context, _ ->
            val implContext = Context(instance.javaClass.simpleName, context)
            implContext.symbolTable = table

            container.context = implContext
        }

        return container
    }

}