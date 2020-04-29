package com.example.webrtcclient_1.utils.extensions

import java.text.DecimalFormat
import kotlin.math.ceil

fun Any.getPercentage(value: Float, total: Int): Float  =
    (value/total)*100f

fun Any.getValueFromPercentage(percentage: Float, total: Int): Int =
    ceil(((percentage/100f)*total).toDouble()).toInt()

private fun getTruncatePercentage(percentage: Double): Float =
    DecimalFormat("#.##").format(percentage).toFloat()