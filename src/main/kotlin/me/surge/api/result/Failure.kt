package me.surge.api.result

import me.surge.lang.error.Error

class Failure(error: Error) : Result(null, error)