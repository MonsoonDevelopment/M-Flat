package me.surge.test

class TestInstantiationClass(val clazz: TestSecondClass?) {

    fun output() {
        println(clazz!!.test)
    }

}