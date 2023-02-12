package me.surge.library

import me.surge.api.result.Result
import me.surge.api.result.Success
import me.surge.lang.value.FunctionData
import me.surge.lang.value.NumberValue
import kotlin.math.ln
import kotlin.math.pow
import kotlin.random.Random

object Maths {

    val pi = Math.PI

    fun pow(functionData: FunctionData, base: NumberValue, exponent: NumberValue): Result {
        return Success(NumberValue(base.name, base.value.toDouble().pow(exponent.value.toDouble())))
    }

    fun sin(value: NumberValue): Result {
        return Success(NumberValue("sinned", kotlin.math.sin(value.value.toFloat())))
    }

    fun cos(value: NumberValue): Result {
        return Success(NumberValue("cosined", kotlin.math.cos(value.value.toFloat())))
    }

    fun tan(value: NumberValue): Result {
        return Success(NumberValue("tanned", kotlin.math.tan(value.value.toFloat())))
    }

    fun asin(value: NumberValue): Result {
        return Success(NumberValue("asinned", kotlin.math.asin(value.value.toFloat())))
    }

    fun acos(value: NumberValue): Result {
        return Success(NumberValue("acosined", kotlin.math.acos(value.value.toFloat())))
    }

    fun atan(value: NumberValue): Result {
        return Success(NumberValue("atanned", kotlin.math.atan(value.value.toFloat())))
    }

    fun toRadians(value: NumberValue): Result {
        return Success(NumberValue("toRadians", Math.toRadians(value.value.toDouble())))
    }

    fun log(value: NumberValue): Result {
        return Success(NumberValue("log", ln(value.value.toDouble())))
    }

    fun sqrt(value: NumberValue): Result {
        return Success(NumberValue("sqrt", kotlin.math.sqrt(value.value.toDouble())))
    }

    fun ceil(value: NumberValue): Result {
        return Success(NumberValue("ceil", kotlin.math.ceil(value.value.toDouble())))
    }

    fun floor(value: NumberValue): Result {
        return Success(NumberValue("floor", kotlin.math.floor(value.value.toDouble())))
    }

    fun abs(value: NumberValue): Result {
        return Success(NumberValue("abs", kotlin.math.abs(value.value.toDouble())))
    }

    fun min(a: NumberValue, b: NumberValue): Result {
        return Success(NumberValue("min", kotlin.math.min(a.value.toDouble(), b.value.toDouble())))
    }

    fun max(a: NumberValue, b: NumberValue): Result {
        return Success(NumberValue("max", kotlin.math.max(a.value.toDouble(), b.value.toDouble())))
    }

    fun round(value: NumberValue): Result {
        return Success(NumberValue("round", kotlin.math.round(value.value.toDouble())))
    }

    fun roundToNearest(value: NumberValue, nearest: NumberValue): Result {
        return Success(NumberValue("roundToNearest", nearest.value.toDouble() * (kotlin.math.round(value.value.toDouble() / nearest.value.toDouble()))))
    }

    fun randomInt(bound: NumberValue): Result {
        return Success(NumberValue("random", Random.nextInt(bound.value.toInt())))
    }

    fun randomFloat(): Result {
        return Success(NumberValue("random", Random.nextFloat()))
    }

}