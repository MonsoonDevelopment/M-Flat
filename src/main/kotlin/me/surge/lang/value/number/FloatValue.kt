package me.surge.lang.value.number

/**
 * @author surge
 * @since 21/02/2023
 */
class FloatValue(identifier: String, value: Float) : NumberValue<Float>(identifier, "float", value) {

    override fun clone(): FloatValue {
        return FloatValue(identifier, value)
            .setPosition(this.start, this.end)
            .setContext(this.context) as FloatValue
    }

}