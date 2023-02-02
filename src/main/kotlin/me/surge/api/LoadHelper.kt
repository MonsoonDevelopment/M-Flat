package me.surge.api

import me.surge.api.annotation.ExcludeFromProcessing
import me.surge.api.annotation.Mutable
import me.surge.api.annotation.OverrideName
import me.surge.lexer.symbol.SymbolTable
import me.surge.lexer.value.*
import me.surge.lexer.value.method.BaseMethodValue
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

            val fieldValue = field.get(instance)

            val value = if (fieldValue != null && isValue(fieldValue::class.java)) {
                fieldValue as Value
            } else {
                when (field.type) {
                    Int::class.java -> {
                        if (fieldValue == null) {
                            NullValue()
                        } else {
                            NumberValue(name, fieldValue as Int)
                        }
                    }

                    Float::class.java, Double::class.java, Long::class.java, Short::class.java -> {
                        if (fieldValue == null) {
                            NullValue()
                        } else {
                            NumberValue(name, fieldValue.toString().toFloat())
                        }
                    }

                    String::class.java -> {
                        if (fieldValue == null) {
                            NullValue()
                        } else {
                            StringValue(name, fieldValue.toString())
                        }
                    }

                    Boolean::class.java -> {
                        if (fieldValue == null) {
                            NullValue()
                        } else {
                            BooleanValue(name, fieldValue.toString().toBooleanStrict())
                        }
                    }

                    ContainerValue::class.java -> {
                        if (fieldValue == null) {
                            NullValue()
                        } else {
                            fieldValue as ContainerValue
                        }
                    }

                    List::class.java, ArrayList::class.java -> {
                        if (fieldValue == null) {
                            NullValue()
                        } else {
                            parseArray(name, field.get(instance) as List<*>)
                        }
                    }

                    null, Void::class.java, Nothing::class.java, Unit::class.java -> {
                        NullValue()
                    }

                    else -> {
                        return@forEach
                    }
                }
            }

            symbolTable.set(name, value, SymbolTable.EntryData(field.getAnnotation(Mutable::class.java) == null, declaration = true, null, null, null))
        }

        instance.javaClass.declaredMethods.forEach { method ->
            if (method.isAnnotationPresent(ExcludeFromProcessing::class.java)) {
                return@forEach
            }

            val method = Coercer.coerceMethod(instance, method)

            symbolTable.set(method.identifier, method, SymbolTable.EntryData(immutable = true, declaration = true, null, null, null))
        }
    }

    fun loadClassAsContainer(instance: Any, symbolTable: SymbolTable) {
        val constructors = hashMapOf<Int, List<BaseMethodValue.Argument>>()

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

        symbolTable.set(instance.javaClass.simpleName, JavaClassLinkValue(instance.javaClass.simpleName, instance.javaClass, instance, constructors), SymbolTable.EntryData(instance.javaClass.getAnnotation(Mutable::class.java) == null, declaration = true, null, null, null))
    }
    
    fun getEquivalentValue(clazz: Class<*>): Class<*> {
        if (isValue(clazz)) {
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
        if (isValue(clazz)) {
            return value
        }

        when (value) {
            is NumberValue -> {
                when (clazz) {
                    Int::class.java -> {
                        return value.stringValue().toInt()
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

    fun isValue(clazz: Class<*>): Boolean {
        return clazz == Value::class.java || clazz.superclass == Value::class.java || clazz.superclass?.superclass == Value::class.java
    }

}