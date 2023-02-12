package me.surge.api.result

import me.surge.lang.error.Error
import me.surge.lang.value.Value

open class Result(val value: Value?, val error: Error?)