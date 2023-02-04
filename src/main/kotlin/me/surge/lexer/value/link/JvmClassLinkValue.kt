package me.surge.lexer.value.link

import me.surge.api.LoadHelper
import me.surge.lexer.error.context.Context
import me.surge.lexer.error.impl.JvmLinkError
import me.surge.lexer.error.impl.RuntimeError
import me.surge.lexer.value.*
import me.surge.lexer.value.method.BaseMethodValue
import me.surge.parse.RuntimeResult

class JvmClassLinkValue(identifier: String, val clazz: Class<*>, instance: Any, val constructors: HashMap<Int, List<Argument>>) : BaseMethodValue(identifier, "JVM CLASS LINK") {

    init {
        LoadHelper.loadClass(instance, this.symbols)
    }

    override fun execute(args: List<Value>, context: Context): RuntimeResult {
        var argNames: List<Argument> = arrayListOf()

        run loop@ {
            constructors.forEach { (_, argumentNames) ->
                val res = this.checkArguments(argumentNames, args)

                if (!res.shouldReturn()) {
                    argNames = argumentNames
                    return@loop
                }
            }
        }

        val constructorArgs = mutableListOf<Any?>()

        args.forEach {
            constructorArgs.add(when (it) {
                is BooleanValue -> it.value
                is JvmClassInstanceValue<*> -> it.instance
                is ListValue -> it.elements
                is NullValue -> null

                is NumberValue -> {
                    if (it.value is Int) {
                        it.value.toInt()
                    } else {
                        it.value.toFloat()
                    }
                }

                is StringValue -> {
                    it.value
                }

                else -> {
                    return RuntimeResult().failure(RuntimeError(
                        this.start!!,
                        this.end!!,
                        "Failed to coerce constructor arguments",
                        this.context!!
                    ))
                }
            })
        }

        val constructor = clazz.constructors.first { it.parameterCount == constructorArgs.size }

        try {
            val classInstance = constructor.newInstance(*constructorArgs.toTypedArray())

            val instance = JvmClassInstanceValue(clazz.simpleName, classInstance)

            return RuntimeResult().success(instance)
        } catch (exception: Exception) {
            return RuntimeResult().failure(JvmLinkError(
                this.start!!,
                this.end!!,
                "Failed to instantiate JVM Class instance!\n\nJVM STACK TRACE WILL FOLLOW\n\n${run {
                    exception.printStackTrace()
                    ""
                }}"
            ))
        }
    }

    override fun execute(args: List<Value>): RuntimeResult {
        return execute(args, generateContext())
    }

    override fun toString(): String {
        return stringValue()
    }

    override fun stringValue(): String {
        return "<JVM CLASS LINK [$identifier]>"
    }

}