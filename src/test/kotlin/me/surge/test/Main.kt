package me.surge.test

import me.surge.api.Executor
import me.surge.lexer.value.ListValue
import java.io.File
import java.io.FileReader

fun main(args: Array<String>) {
    val executor = Executor()
        .loadClass("testing", Testing::class.java)
        .loadClass("java_types", JavaTypesTesting::class.java)
        .loadClassAsContainer(TestInstantiationClass(null))
        .loadClassAsContainer(TestSecondClass(""))

    val file = File("modules.mfl")
    val reader = FileReader(file)

    val result = executor.evaluate(file.name, reader.readText())

    if (result.second != null) {
        println(result.second)
        return
    }

    val trueResult = executor.getFunction("main")?.execute(arrayListOf(ListValue("args", arrayListOf())))
        ?: run {
            println("Couldn't find main function!")
            return
        }

    if (trueResult.error != null) {
        println(trueResult.error.toString())
    }

    /* val trueResult = executor.getFunction("loadModules")?.execute(arrayListOf())
        ?: run {
            println("Couldn't find load function!")
            return
        }

    if (trueResult.error != null) {
        println(trueResult.error.toString())
    } else {
        val result = trueResult.value

        result as ListValue

        result.elements.forEach {
            it as ContainerValue<*>

            val table = it.value as SymbolTable

            println(table.get("name"))
            println(table.get("description"))

            table.get("onUpdate")?.execute()
        }
    } */
}
