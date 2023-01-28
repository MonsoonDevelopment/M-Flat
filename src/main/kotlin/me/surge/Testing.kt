package me.surge

import me.surge.api.annotation.Mutable

object Testing {

    val helloWorld = "Hello, world!"
    val bool = true
    val array = arrayOf("hello", "world")
    val twoDArray = arrayOf(arrayOf("test"), arrayOf("test2"))

    @Mutable
    val integer = 0

    fun test() {
        println("called from test function")
    }

}