package me.surge.lexer.value

@ValueName("instance")
class ContainerInstanceValue<T>(name: String, val value: T) : Value(name) {

    override fun clone(): Value {
        return ContainerInstanceValue(name, value)
            .setContext(context)
            .setPosition(this.start, this.end)
    }

}