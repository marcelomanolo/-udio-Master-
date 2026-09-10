package com.manobrown.audiomaster

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

class DspChain {
    @Volatile var preampManual: Float = 0f
    @Volatile var headroomAuto: Boolean = true
    @Volatile var bass: Float = 0.2f
    @Volatile var vocal: Float = 0.1f
    @Volatile var width: Float = 0f
    @Volatile var limiterAtivo: Boolean = true

    private val ganhos = FloatArray(10) { 0f }
    private val bqs = Array(10) { Biquad() }
    private val bassFilter = Biquad()
    private val vocalHpf = Biquad()
    private val vocalPk = Biquad()
    private val fc = floatArrayOf(31.25f, 62.5f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
    private val qPadrao = floatArrayOf(0.7f, 0.7f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.7f, 0.7f)
    private var sr = -1

    @Volatile var atenuacaoAtualDb: Float = 0f
    @Volatile var headroomAplicadoDb: Float = 0f
    @Volatile var ultimoPicoInterAmostraDb: Float = -120f

    fun setBanda(i: Int, gDb: Float) {
        if (i in 0..9) {
            ganhos[i] = gDb
            if (sr > 0) bqs[i].design(
                fs = sr.toDouble(),
                f0 = fc[i].toDouble(),
                Q = qPadrao[i].toDouble(),
                gainDb = gDb.toDouble(),
                tipo = Biquad.PEAK
            )
        }
    }

    private fun projetar() {
        for (i in 0..9) bqs[i].design(
            fs = sr.toDouble(), f0 = fc[i].toDouble(),
            Q = qPadrao[i].toDouble(), gainDb = ganhos[i].toDouble(), tipo = Biquad.PEAK
        )
        bassFilter.design(fs = sr.toDouble(), f0 = 80.0, Q = 0.707,
            gainDb = (bass * 6.0).toDouble(), tipo = Biquad.LOW_SHELF)
        vocalHpf.design(fs = sr.toDouble(), f0 = 180.0, Q = 0.707,
            gainDb = 0.0, tipo = Biquad.HIGH_PASS)
        vocalPk.design(fs = sr.toDouble(), f0 = 3200.0, Q = 1.0,
            gainDb = (vocal * 3.0).toDouble(), tipo = Biquad.PEAK)
    }

    fun processar(buf: FloatArray, frames: Int, sampleRate: Int): FloatArray {
        if (sampleRate != sr) { sr = sampleRate; projetar() }

        val ganhoMax = (0..9).maxOf { ganhos[it] }.coerceAtLeast(0f)
            .plus(bass * 6f).plus(vocal * 3f)
        val preDb = if (headroomAuto) max(-18f, minOf(0f, -2f - 3f - ganhoMax)) else preampManual
        val pre = 10.0.pow(preDb / 20.0).toFloat()
        headroomAplicadoDb = -preDb

        var pico = 0f
        for (i in 0 until frames) {
            val il = 2 * i; val ir = 2 * i + 1
            var l = buf[il] * pre; var r = buf[ir] * pre
            for (b in 0..9) { l = bqs[b].processar(l); r = bqs[b].processar(r) }
            l = bassFilter.processar(l); r = bassFilter.processar(r)
            l = vocalHpf.processar(l);   r = vocalHpf.processar(r)
            l = vocalPk.processar(l);    r = vocalPk.processar(r)
            if (kotlin.math.abs(width) > 1e-3f) {
                val m = 0.5f * (l + r); val s = 0.5f * (l - r) * (1f + width * 0.5f)
                l = m + s; r = m - s
            }

            if (i + 1 < frames) {
                pico = max(pico, kotlin.math.abs(l), kotlin.math.abs(r), kotlin.math.abs(buf[il]), kotlin.math.abs(buf[ir]))
            } else pico = max(pico, kotlin.math.abs(l), kotlin.math.abs(r))

            if (limiterAtivo && pico > 0.89f) {
                val g = 0.89f / pico
                val target = (20 * log10(g.toDouble())).toFloat()
                atenuacaoAtualDb += (target - atenuacaoAtualDb) * 0.05f
                val k = 10.0.pow(atenuacaoAtualDb / 20.0).toFloat()
                l *= k; r *= k
            } else if (limiterAtivo) {
                atenuacaoAtualDb += (0f - atenuacaoAtualDb) * 0.0008f
            }
            buf[il] = l; buf[ir] = r
        }
        ultimoPicoInterAmostraDb = if (pico > 1e-9f) (20 * log10(pico)).toFloat() else -120f
        return buf
    }

    fun reset() {
        for (b in 0..9) bqs[b].reset()
        bassFilter.reset(); vocalHpf.reset(); vocalPk.reset()
    }

    private fun minOf(a: Float, b: Float) = if (a < b) a else b
}
