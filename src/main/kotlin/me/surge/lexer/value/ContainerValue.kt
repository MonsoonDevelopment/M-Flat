package me.surge.lexer.value

class ContainerValue<T>(name: String, val value: T) : Value(name) {

    override fun clone(): Value {
        return ContainerValue(name, value)
            .setContext(context)
            .setPosition(this.start, this.end)
    }

}