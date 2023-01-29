package me.surge.api

import me.surge.api.annotation.ExcludeFromProcessing
import me.surge.api.annotation.Mutable
import me.surge.api.annotation.OverrideName
import me.surge.api.result.Failure
import me.surge.api.result.Success
import me.surge.lexer.error.impl.RuntimeError
import me.surge.lexer.symbol.SymbolTable
import me.surge.lexer.value.*
import me.surge.lexer.value.function.BuiltInFunction
import me.surge.parse.RuntimeResult
import me.surge.util.firstIndexed
import java.lang.IllegalStateException

object LoadHelper {

    fun loadClass(instance: Any, symbolTable: SymbolTable) {
        val instance: Any = if (instance is Class<*>) {
            val constructor = instance.getDeclaredConstructor()
            constructor.isAccessible = true
            constructor.newInstance()
        } else {
            instance
        }

        instance.javaClass.declaredFields.forEach { field ->
            if (field.getAnnotation(ExcludeFromProcessing::class.java) != null) {
                return@forEach
            }

            field.isAccessible = true

            val name = if (field.isAnnotationPresent(OverrideName::class.java)) {
                field.getAnnotation(OverrideName::class.java).name
            } else {
                field.name
            }

            fun parseArray(name: String, array: Array<*>): ListValue {
                val elements = ArrayList<Value>()

                array.forEach {
                    elements.add(
                        when (it) {
                            is Int -> {
                                NumberValue(array.size.toString(), it)
                            }

                            is Float, is Double, is Long, is Short -> {
                                NumberValue(array.size.toString(), it.toString().toFloat())
                            }

                            is String -> {
                                StringValue(array.size.toString(), it.toString())
                            }

                            is Boolean -> {
                                NumberValue(array.size.toString(), if (it) 1 else 0)
                            }

                            else -> {
                                if (it!!.javaClass.isArray) {
                                    parseArray(array.size.toString(), it as Array<*>)
                                } else {
                                    Value(array.size.toString())
                                }
                            }
                        }
                    )
                }

                return ListValue(name, elements)
            }

            symbolTable.set(name,
                when (field.type) {
                    Int::class.java -> {
                        NumberValue(name, field.getInt(instance))
                    }

                    Float::class.java, Double::class.java, Long::class.java, Short::class.java -> {
                        NumberValue(name, field.get(instance).toString().toFloat())
                    }

                    String::class.java -> {
                        StringValue(name, field.get(instance).toString())
                    }

                    Boolean::class.java -> {
                        BooleanValue(name, field.getBoolean(instance))
                    }

                    else -> {
                        if (field.type.isArray) {
                            parseArray(name, field.get(instance) as Array<*>)
                        } else {
                            Value(name)
                        }
                    }
                },
                SymbolTable.EntryData(field.getAnnotation(Mutable::class.java) == null, declaration = true, null, null, null)
            )
        }

        instance.javaClass.declaredMethods.forEach { method ->
            if (method.isAnnotationPresent(ExcludeFromProcessing::class.java)) {
                return@forEach
            }

            method.isAccessible = true

            val name = if (method.isAnnotationPresent(OverrideName::class.java)) {
                method.getAnnotation(OverrideName::class.java).name
            } else {
                method.name
            }

            val function = builtIn(
                name,

                { functionData ->
                    val types = method.parameterTypes.filter { it != FunctionData::class.java }
                    val converted = arrayListOf<Class<*>>()

                    types.forEach {
                        converted.add(getEquivalentValue(it))
                    }

                    val arguments = arrayListOf<Any>()

                    if (method.parameters.any { it.type == FunctionData::class.java }) {
                        arguments.add(functionData)
                    }

                    functionData.arguments.forEachIndexed { index, value ->
                        arguments.add(getEquivalentPrimitive(value, types[index]))
                    }

                    val result = method.invoke(instance, *arguments.toTypedArray())

                    if (result is RuntimeResult) {
                        return@builtIn result
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

            symbolTable.set(name, function, SymbolTable.EntryData(immutable = true, declaration = true, null, null, null))
        }
    }

    private fun builtIn(name: String, method: (functionData: FunctionData) -> RuntimeResult, argumentNames: ArrayList<String>): BuiltInFunction {
        return BuiltInFunction(name, method, argumentNames)
    }

    private fun getEquivalentValue(clazz: Class<*>): Class<*> {
        if (clazz.superclass == Value::class.java || clazz == Value::class.java) {
            return clazz
        }

        when (clazz) {
            Int::class.java, Float::class.java, Double::class.java, Long::class.java, Short::class.java -> {
                return NumberValue::class.java
            }

            String::class.java -> {
                return StringValue::class.java
            }

            Boolean::class.java -> {
                return BooleanValue::class.java
            }
        }

        throw IllegalStateException("No equivalent value found! (Got $clazz)")
    }

    private fun getEquivalentPrimitive(value: Value, clazz: Class<*>): Any {
        if (clazz.superclass == Value::class.java || clazz == Value::class.java) {
            return value
        }

        when (value) {
            is NumberValue -> {
                when (clazz) {
                    Int::class.java -> {
                        return value.rawValue().toInt()
                    }

                    Float::class.java -> {
                        return value.value.toFloat()
                    }

                    Double::class.java -> {
                        return value.value.toDouble()
                    }

                    Long::class.java -> {
                        return value.value.toLong()
                    }

                    Short::class.java -> {
                        return value.value.toShort()
                    }
                }
            }

            is StringValue -> {
                return value.value
            }

            is BooleanValue -> {
                return value.value
            }
        }

        throw IllegalStateException("No equivalent primitive found! (Got $value, $clazz)")
    }

}