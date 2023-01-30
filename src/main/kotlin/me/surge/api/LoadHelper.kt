package me.surge.api

import me.surge.api.annotation.ExcludeFromProcessing
import me.surge.api.annotation.Mutable
import me.surge.api.annotation.OverrideName
import me.surge.lexer.symbol.SymbolTable
import me.surge.lexer.value.*
import me.surge.lexer.value.function.BuiltInFunction
import me.surge.parse.RuntimeResult
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
            if (field.getAnnotation(ExcludeFromProcessing::class.java) != null || field.name == "INSTANCE") {
                return@forEach
            }

            field.isAccessible = true

            val name = if (field.isAnnotationPresent(OverrideName::class.java)) {
                field.getAnnotation(OverrideName::class.java).name
            } else {
                field.name
            }

            fun parseArray(name: String, array: List<*>): ListValue {
                val elements = ArrayList<Value>()

                array.forEach arrayFor@{
                    elements.add(
                        when (it) {
                            is Int -> {
                                NumberValue(array.size.toString(), it)
                            }

                            is Number -> {
                                NumberValue(array.size.toString(), it.toString().toFloat())
                            }

                            is String -> {
                                StringValue(array.size.toString(), it.toString())
                            }

                            is Boolean -> {
                                NumberValue(array.size.toString(), if (it) 1 else 0)
                            }

                            is List<*> -> {
                                parseArray(array.size.toString(), it)
                            }

                            else -> {
                                return@arrayFor
                            }
                        }
                    )
                }

                return ListValue(name, elements)
            }

            var value = when (field.type) {
                Int::class.java -> {
                    val fieldValue = field.get(instance)

                    if (fieldValue == null) {
                        NumberValue.NULL
                    } else {
                        NumberValue(name, fieldValue as Int)
                    }
                }

                Float::class.java, Double::class.java, Long::class.java, Short::class.java -> {
                    val fieldValue = field.get(instance)

                    if (fieldValue == null) {
                        NumberValue.NULL
                    } else {
                        NumberValue(name, fieldValue.toString().toFloat())
                    }
                }

                String::class.java -> {
                    val fieldValue = field.get(instance)

                    if (fieldValue == null) {
                        NumberValue.NULL
                    } else {
                        StringValue(name, fieldValue.toString())
                    }
                }

                Boolean::class.java -> {
                    val fieldValue = field.get(instance)

                    if (fieldValue == null) {
                        NumberValue.NULL
                    } else {
                        BooleanValue(name, fieldValue.toString().toBooleanStrict())
                    }
                }

                ContainerValue::class.java -> {
                    val fieldValue = field.get(instance)

                    if (fieldValue == null) {
                        NumberValue.NULL
                    } else {
                        fieldValue as ContainerValue
                    }
                }

                List::class.java, ArrayList::class.java -> {
                    val fieldValue = field.get(instance)

                    if (fieldValue == null) {
                        NumberValue.NULL
                    } else {
                        parseArray(name, field.get(instance) as List<*>)
                    }
                }

                else -> {
                    return@forEach
                }
            }

            if (field::class.java.isInstance(Value::class.java)) {
                value = field.get(value) as Value

                println(value)
            }

            symbolTable.set(name, value, SymbolTable.EntryData(field.getAnnotation(Mutable::class.java) == null, declaration = true, null, null, null))
        }

        instance.javaClass.declaredMethods.forEach { method ->
            if (method.isAnnotationPresent(ExcludeFromProcessing::class.java)) {
                return@forEach
            }

            val method = Coercer.coerceMethod(instance, method)

            symbolTable.set(method.name, method, SymbolTable.EntryData(immutable = true, declaration = true, null, null, null))
        }
    }

    fun getEquivalentValue(clazz: Class<*>): Class<*> {
        if (isAssignableFrom(clazz, Value::class.java)) {
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

    fun getEquivalentPrimitive(value: Value, clazz: Class<*>): Any {
        if (isAssignableFrom(clazz, Value::class.java)) {
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

    fun isAssignableFrom(clazz: Class<*>, superclass: Class<*>): Boolean {
        var assignable = clazz == superclass

        if (!assignable) {
            var superclass = clazz.superclass

            while (superclass != null && !assignable) {
                assignable = clazz == superclass
                superclass = clazz.superclass
            }
        }

        return assignable
    }

}