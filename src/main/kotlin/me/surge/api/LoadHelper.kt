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

object LoadHelper {

    fun loadClass(instance: Any, symbolTable: SymbolTable) {
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
                        NumberValue(name, if (field.getBoolean(instance)) 1 else 0)
                    }

                    else -> {
                        if (field.type.isArray) {
                            parseArray(name, field.get(instance) as Array<*>)
                        } else {
                            Value(name)
                        }
                    }
                },
                field.getAnnotation(Mutable::class.java) == null, declaration = true
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

                    val firstNotMatchingType = functionData.arguments.firstIndexed { index, element -> element!!::class.java != types[index] && types[index] != Value::class.java }

                    if (firstNotMatchingType != null) {
                        return@builtIn RuntimeResult().failure(
                            RuntimeError(
                            functionData.start,
                            functionData.end,
                            "Invalid argument (${functionData.arguments.indexOf(firstNotMatchingType)}) type! Got ${(firstNotMatchingType as Value)::class.java.getAnnotation(
                                ValueName::class.java).name}, expected ${types[functionData.arguments.indexOf(firstNotMatchingType)].simpleName.replace("Value", "").lowercase()}.",
                            functionData.context
                        ))
                    }

                    val arguments = arrayListOf<Any>()

                    if (method.parameters.any { it.type == FunctionData::class.java }) {
                        arguments.add(functionData)
                    }

                    arguments.addAll(functionData.arguments)

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

                        else -> {
                            RuntimeResult().success(NumberValue.NULL)
                        }
                    }
                },

                ArrayList(method.parameters.filter { it.type != FunctionData::class.java }.map { it.name }.toList())
            )

            symbolTable.set(name, function, true, declaration = true)
        }
    }

    fun loadClass(clazz: Class<*>, symbolTable: SymbolTable) {
        clazz.getDeclaredConstructor().isAccessible = true
        val instance = clazz.getDeclaredConstructor().newInstance()

        loadClass(instance, symbolTable)
    }

    private fun builtIn(name: String, method: (functionData: FunctionData) -> RuntimeResult, argumentNames: ArrayList<String>): BuiltInFunction {
        return BuiltInFunction(name, method, argumentNames)
    }

}