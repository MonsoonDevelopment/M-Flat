package me.surge.library

import me.surge.api.annotation.OverrideName

object BuiltIn {

    @OverrideName("null") val NULL = 0

    @OverrideName("true") val TRUE = true

    @OverrideName("false") val FALSE = true

    val pi = Math.PI

}