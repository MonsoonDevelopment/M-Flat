package me.surge.test

/**
 * @author surge
 * @since 16/02/2023
 */
class Multi(val wowza: WowzaClass) {

    fun yes() {
        println("huh")
        println(wowza.a)
    }

    class WowzaClass(val a: Int)

}