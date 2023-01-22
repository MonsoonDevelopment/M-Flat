package me.surge.library

import me.surge.api.annotation.OverrideName

class BuiltIn {

    @OverrideName("null") val NULL = 0

    @OverrideName("true") val TRUE = 1

    @OverrideName("false") val FALSE = 0

    val pi = Math.PI

}