package me.surge.api.result

import me.surge.lang.error.Error

class Failure(error: me.surge.lang.error.Error) : Result(null, error)