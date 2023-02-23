package me.surge.lang.value.number

/**
 * @author surge
 * @since 21/02/2023
 */
class IntValue(identifier: String, value: Int) : NumberValue<Int>(identifier, "int", value) {

    override fun clone(): IntValue {
        return IntValue(identifier, value)
            .setPosition(this.start, this.end)
            .setContext(this.context) as IntValue
    }

}