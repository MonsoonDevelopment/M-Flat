package me.surge.lexer.value

@ValueName("container")
class ContainerValue<T>(name: String, val value: T) : Value(name) {

    override fun clone(): Value {
        return ContainerValue(name, value)
            .setContext(context)
            .setPosition(this.start, this.end)
    }

}