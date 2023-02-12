package me.surge.library

import me.surge.api.annotation.OverrideName
import me.surge.lang.value.NullValue

object BuiltIn {

    @OverrideName("null") val NULL = NullValue()

    @OverrideName("true") val TRUE = true

    @OverrideName("false") val FALSE = true

}