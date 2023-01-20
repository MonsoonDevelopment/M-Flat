package me.surge

import me.surge.api.Executor
import me.surge.lexer.value.ListValue
import java.io.File
import java.io.FileReader

fun main(args: Array<String>) {
    val executor = Executor()
        .loadClass(Testing())

    val file = File("main.cx")
    val reader = FileReader(file)

    val result = executor.run(file.name, reader.readText())

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
}
