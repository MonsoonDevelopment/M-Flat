package me.surge.test

import me.surge.api.Coercer
import me.surge.api.annotation.Mutable
import me.surge.api.annotation.OverrideName
import me.surge.lexer.value.link.JvmClassInstanceValue

object Testing {

    val helloWorld = "Hello, world!"
    val bool = true
    val array = arrayListOf("hello", "world")
    val twoDArray = arrayListOf(arrayListOf("test"), arrayListOf("test2"))

    @Mutable
    val integer = 0

    fun test() {
        println("called from test function")
    }

    fun test2(): Boolean {
        return false
    }

    fun loadTest(value: JvmClassInstanceValue<TestInstantiationClass>) {
        println("A = " + value.instance.clazz!!.test)
    }

    @OverrideName("TestClass")
    val testClass = Coercer.createContainer(TestClass(5))

    class TestClass(val a: Int) {
        fun testClassFunction() {
            println("test calss functionsdfds")
        }
    }

}