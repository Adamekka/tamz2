package com.adamekka.tamz

import kotlin.math.roundToInt

data class SliderRange(val maximum: Int, val increment: Int) {
    init {
        assert(maximum > 0 && increment > 0 && maximum % increment == 0) {
            "Slider maximum must be a positive multiple of its increment"
        }
    }

    fun snap(value: Double): Double =
        ((value.coerceIn(0.0, maximum.toDouble()) / increment).roundToInt() * increment).toDouble()
}
