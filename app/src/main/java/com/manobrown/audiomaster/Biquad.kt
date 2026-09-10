package com.manobrown.audiomaster

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Biquad {
    companion object {
        const val PEAK = 0
        const val LOW_SHELF = 1
        const val HIGH_SHELF = 2
        const val HIGH_PASS = 3
    }

    private var b0 = 1.0f
    private var b1 = 0.0f
    private var b2 = 0.0f
    private var a1 = 0.0f
    private var a2 = 0.0f
    private var w1 = 0.0f
    private var w2 = 0.0f

    fun design(fs: Double, f0: Double, Q: Double, gainDb: Double, tipo: Int) {
        val w0 = 2.0 * PI * f0 / fs
        val cosw0 = cos(w0)
        val sinw0 = sin(w0)
        val alpha = sinw0 / (2.0 * Q)
        val A = 10.0.pow(gainDb / 40.0)

        when (tipo) {
            PEAK -> {
                val nb0 = 1.0 + alpha * A
                val nb1 = -2.0 * cosw0
                val nb2 = 1.0 - alpha * A
                val a0 = 1.0 + alpha / A
                val na1 = -2.0 * cosw0
                val na2 = 1.0 - alpha / A
                val norm = 1.0 / a0
                this.b0 = (nb0 * norm).toFloat()
                this.b1 = (nb1 * norm).toFloat()
                this.b2 = (nb2 * norm).toFloat()
                this.a1 = (-na1 * norm).toFloat()
                this.a2 = (-na2 * norm).toFloat()
            }
            LOW_SHELF -> {
                val sqrtA = sqrt(A)
                val nb0 = A * ((A + 1) - (A - 1) * cosw0 + 2 * sqrtA * alpha)
                val nb1 = 2.0 * A * ((A - 1) - (A + 1) * cosw0)
                val nb2 = A * ((A + 1) - (A - 1) * cosw0 - 2 * sqrtA * alpha)
                val a0 = (A + 1) + (A - 1) * cosw0 + 2 * sqrtA * alpha
                val na1 = -2.0 * ((A - 1) + (A + 1) * cosw0)
                val na2 = (A + 1) + (A - 1) * cosw0 - 2 * sqrtA * alpha
                val norm = 1.0 / a0
                this.b0 = (nb0 * norm).toFloat()
                this.b1 = (nb1 * norm).toFloat()
                this.b2 = (nb2 * norm).toFloat()
                this.a1 = (-na1 * norm).toFloat()
                this.a2 = (-na2 * norm).toFloat()
            }
            HIGH_PASS -> {
                val nb0 = (1.0 + cosw0) / 2.0
                val nb1 = -(1.0 + cosw0)
                val nb2 = (1.0 + cosw0) / 2.0
                val a0 = 1.0 + alpha
                val na1 = -2.0 * cosw0
                val na2 = 1.0 - alpha
                val norm = 1.0 / a0
                this.b0 = (nb0 * norm).toFloat()
                this.b1 = (nb1 * norm).toFloat()
                this.b2 = (nb2 * norm).toFloat()
                this.a1 = (-na1 * norm).toFloat()
                this.a2 = (-na2 * norm).toFloat()
            }
        }
        w1 = 0f; w2 = 0f
    }

    fun processar(x: Float): Float {
        val y = b0 * x + b1 * w1 + b2 * w2 - a1 * w1 - a2 * w2
        w2 = w1
        w1 = x
        return y
    }

    fun reset() { w1 = 0f; w2 = 0f }
}
