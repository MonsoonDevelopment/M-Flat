package me.surge.lang.value.link

import me.surge.api.LoadHelper
import me.surge.lang.error.context.Context
import me.surge.lang.error.impl.JvmLinkError
import me.surge.lang.error.impl.RuntimeError
import me.surge.lang.parse.RuntimeResult
import me.surge.lang.util.Link
import me.surge.lang.value.*
import me.surge.lang.value.method.BaseMethodValue

class JvmClassLinkValue(identifier: String, val clazz: Class<*>, instance: Any, val constructors: HashMap<Int, List<Argument>>) : BaseMethodValue(identifier, "JVM CLASS LINK"),
    Link {

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
                    return RuntimeResult().failure(
                        RuntimeError(
                            this.start!!,
                            this.end!!,
                            "Failed to coerce constructor arguments",
                            this.context!!
                        )
                    )
                }
            })
        }

        val constructor = clazz.constructors.first { it.parameterCount == constructorArgs.size }

        try {
            val classInstance = constructor.newInstance(*constructorArgs.toTypedArray())

            val instance = JvmClassInstanceValue(clazz.simpleName, classInstance)

            return RuntimeResult().success(instance)
        } catch (exception: Exception) {
            return RuntimeResult().failure(
                JvmLinkError(
                    this.start!!,
                    this.end!!,
                    "Failed to instantiate JVM Class instance!\n\nJVM STACK TRACE WILL FOLLOW\n\n${
                        run {
                            exception.printStackTrace()
                            ""
                        }
                    }"
                )
            )
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