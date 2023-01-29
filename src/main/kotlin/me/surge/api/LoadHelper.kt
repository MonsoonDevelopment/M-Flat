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

            var value = when (field.type) {
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

                ContainerValue::class.java -> {
                    field.get(instance) as ContainerValue
                }

                else -> {
                    if (field.type.isArray) {
                        parseArray(name, field.get(instance) as Array<*>)
                    } else {
                        Value(name)
                    }
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