package me.surge.library

import me.surge.api.result.Result
import me.surge.api.result.Success
import me.surge.lang.util.inferNumberValue
import me.surge.lang.value.FunctionData
import me.surge.lang.value.number.IntValue
import me.surge.lang.value.number.NumberValue
import kotlin.math.ln
import kotlin.math.pow
import kotlin.random.Random

object Maths {

    val pi = Math.PI

    fun pow(functionData: FunctionData, base: NumberValue<*>, exponent: IntValue): Result {
        return Success(inferNumberValue(base.name, base.value.toDouble().pow(exponent.value.toDouble())))
    }

    fun sin(value: NumberValue<*>): Result {
        return Success(inferNumberValue("sinned", kotlin.math.sin(value.value.toFloat())))
    }

    fun cos(value: NumberValue<*>): Result {
        return Success(inferNumberValue("cosined", kotlin.math.cos(value.value.toFloat())))
    }

    fun tan(value: NumberValue<*>): Result {
        return Success(inferNumberValue("tanned", kotlin.math.tan(value.value.toFloat())))
    }

    fun asin(value: NumberValue<*>): Result {
        return Success(inferNumberValue("asinned", kotlin.math.asin(value.value.toFloat())))
    }

    fun acos(value: NumberValue<*>): Result {
        return Success(inferNumberValue("acosined", kotlin.math.acos(value.value.toFloat())))
    }

    fun atan(value: NumberValue<*>): Result {
        return Success(inferNumberValue("atanned", kotlin.math.atan(value.value.toFloat())))
    }

    fun toRadians(value: NumberValue<*>): Result {
        return Success(inferNumberValue("toRadians", Math.toRadians(value.value.toDouble())))
    }

    fun log(value: NumberValue<*>): Result {
        return Success(inferNumberValue("log", ln(value.value.toDouble())))
    }

    fun sqrt(value: NumberValue<*>): Result {
        return Success(inferNumberValue("sqrt", kotlin.math.sqrt(value.value.toDouble())))
    }

    fun ceil(value: NumberValue<*>): Result {
        return Success(inferNumberValue("ceil", kotlin.math.ceil(value.value.toDouble())))
    }

    fun floor(value: NumberValue<*>): Result {
        return Success(inferNumberValue("floor", kotlin.math.floor(value.value.toDouble())))
    }

    fun abs(value: NumberValue<*>): Result {
        return Success(inferNumberValue("abs", kotlin.math.abs(value.value.toDouble())))
    }

    fun min(a: NumberValue<*>, b: NumberValue<*>): Result {
        return Success(inferNumberValue("min", kotlin.math.min(a.value.toDouble(), b.value.toDouble())))
    }

    fun max(a: NumberValue<*>, b: NumberValue<*>): Result {
        return Success(inferNumberValue("max", kotlin.math.max(a.value.toDouble(), b.value.toDouble())))
    }

    fun round(value: NumberValue<*>): Result {
        return Success(inferNumberValue("round", kotlin.math.round(value.value.toDouble())))
    }

    fun roundToNearest(value: NumberValue<*>, nearest: NumberValue<*>): Result {
        return Success(inferNumberValue("roundToNearest", nearest.value.toDouble() * (kotlin.math.round(value.value.toDouble() / nearest.value.toDouble()))))
    }

    fun randomInt(bound: NumberValue<*>): Result {
        return Success(inferNumberValue("random", Random.nextInt(bound.value.toInt())))
    }

    fun randomFloat(): Result {
        return Success(inferNumberValue("random", Random.nextFloat()))
    }

}