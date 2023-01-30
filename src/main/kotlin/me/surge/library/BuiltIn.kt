package me.surge.library

import me.surge.api.annotation.OverrideName
import me.surge.lexer.value.NumberValue

object BuiltIn {

    @OverrideName("null") val NULL = NumberValue.NULL

    @OverrideName("true") val TRUE = true

    @OverrideName("false") val FALSE = true

    val pi = Math.PI

}