package com.mahjongzen.audio

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Tiny procedural synthesiser. All sounds of the app are generated here at runtime –
 * no third-party audio files are used (royalty-free by construction).
 */
object Synth {
    const val RATE = 44_100

    private fun buffer(seconds: Double) = FloatArray((seconds * RATE).toInt())

    /** Adds a decaying bell-like tone with a few harmonics. */
    private fun bell(out: FloatArray, start: Double, freq: Double, amp: Double, decay: Double, rate: Int = RATE) {
        val s0 = (start * rate).toInt()
        val attack = 0.004 * rate
        for (i in s0 until out.size) {
            val t = (i - s0).toDouble() / rate
            val env = exp(-t / decay) * minOf(1.0, (i - s0) / attack)
            if (env < 0.0005 && t > 0.05) break
            val v = sin(2 * PI * freq * t) + 0.35 * sin(2 * PI * freq * 2.01 * t) * exp(-t / (decay * 0.5)) +
                0.12 * sin(2 * PI * freq * 3.98 * t) * exp(-t / (decay * 0.25))
            out[i] += (v * env * amp).toFloat()
        }
    }

    private fun noise(out: FloatArray, start: Double, length: Double, amp: Double, rng: Random, smooth: Double) {
        val s0 = (start * RATE).toInt()
        val n = (length * RATE).toInt()
        var lp = 0.0
        for (k in 0 until n) {
            val i = s0 + k
            if (i >= out.size) break
            val t = k.toDouble() / n
            val env = sin(PI * t) * sin(PI * t)
            lp += (rng.nextDouble(-1.0, 1.0) - lp) * smooth
            out[i] += (lp * env * amp).toFloat()
        }
    }

    fun select(): FloatArray = buffer(0.12).also {
        bell(it, 0.0, 880.0, 0.35, 0.025)
        bell(it, 0.0, 1320.0, 0.15, 0.018)
        noise(it, 0.0, 0.012, 0.25, Random(1), 0.5)
    }

    fun match(): FloatArray = buffer(0.7).also {
        bell(it, 0.0, 659.25, 0.35, 0.18)
        bell(it, 0.07, 987.77, 0.30, 0.25)
    }

    fun combo(): FloatArray = buffer(0.9).also {
        bell(it, 0.0, 783.99, 0.28, 0.15)
        bell(it, 0.06, 987.77, 0.26, 0.18)
        bell(it, 0.12, 1174.66, 0.26, 0.22)
        bell(it, 0.18, 1567.98, 0.18, 0.3)
    }

    fun invalid(): FloatArray = buffer(0.2).also {
        bell(it, 0.0, 196.0, 0.4, 0.05)
        bell(it, 0.0, 233.08, 0.25, 0.04)
    }

    fun hint(): FloatArray = buffer(0.6).also {
        bell(it, 0.0, 1318.5, 0.22, 0.2)
        bell(it, 0.12, 1318.5, 0.12, 0.2)
    }

    fun shuffle(): FloatArray = buffer(0.5).also {
        val rng = Random(7)
        for (k in 0 until 6) noise(it, k * 0.06, 0.09, 0.22, rng, 0.25)
    }

    fun undo(): FloatArray = buffer(0.3).also {
        bell(it, 0.0, 587.33, 0.25, 0.08)
        bell(it, 0.07, 440.0, 0.25, 0.1)
    }

    fun win(): FloatArray = buffer(2.6).also {
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.5, 1318.5, 1567.98)
        notes.forEachIndexed { k, f -> bell(it, k * 0.11, f, 0.22, 0.9) }
        bell(it, 0.8, 523.25, 0.15, 1.2)
        bell(it, 0.8, 783.99, 0.12, 1.2)
    }

    /**
     * A calm, seamlessly looping ambient piece: soft pentatonic pad chords plus sparse plucks.
     * Everything is computed modulo the loop length, so the end blends into the start.
     */
    fun music(): FloatArray {
        val rate = 22_050
        val seconds = 32.0
        val out = FloatArray((seconds * rate).toInt())
        val n = out.size
        // D major pentatonic-ish chord progression
        val chords = listOf(
            doubleArrayOf(146.83, 220.0, 293.66, 369.99),
            doubleArrayOf(123.47, 185.0, 246.94, 293.66),
            doubleArrayOf(110.0, 164.81, 220.0, 329.63),
            doubleArrayOf(130.81, 196.0, 261.63, 329.63),
        )
        val chordLen = seconds / chords.size
        val window = chordLen * 1.6
        for ((c, chord) in chords.withIndex()) {
            val start = c * chordLen
            val wn = (window * rate).toInt()
            for (k in 0 until wn) {
                val i = ((start * rate).toInt() + k) % n
                val t = k.toDouble() / rate
                val env = sin(PI * k / wn).let { it * it } * 0.06
                var v = 0.0
                for ((j, f) in chord.withIndex()) {
                    v += sin(2 * PI * f * t + j) * (1.0 - j * 0.15)
                    v += 0.3 * sin(2 * PI * f * 1.003 * t)
                }
                out[i] += (v * env).toFloat()
            }
        }
        // sparse plucks
        val scale = doubleArrayOf(587.33, 659.25, 739.99, 880.0, 987.77, 1174.66)
        val rng = Random(2024)
        var t = 1.0
        while (t < seconds - 0.5) {
            val f = scale[rng.nextInt(scale.size)]
            val s0 = (t * rate).toInt()
            val len = (2.5 * rate).toInt()
            for (k in 0 until len) {
                val i = (s0 + k) % n
                val tt = k.toDouble() / rate
                out[i] += (sin(2 * PI * f * tt) * exp(-tt / 0.7) * 0.07 * minOf(1.0, k / 60.0)).toFloat()
            }
            t += 1.5 + rng.nextDouble() * 2.5
        }
        return out
    }

    /** Writes 16-bit mono PCM WAV. */
    fun writeWav(file: File, samples: FloatArray, rate: Int = RATE) {
        var peak = 0f
        for (s in samples) peak = maxOf(peak, kotlin.math.abs(s))
        val gain = if (peak > 0.98f) 0.98f / peak else 1f
        val dataLen = samples.size * 2
        val bb = ByteBuffer.allocate(44 + dataLen).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray()); bb.putInt(36 + dataLen); bb.put("WAVE".toByteArray())
        bb.put("fmt ".toByteArray()); bb.putInt(16); bb.putShort(1); bb.putShort(1)
        bb.putInt(rate); bb.putInt(rate * 2); bb.putShort(2); bb.putShort(16)
        bb.put("data".toByteArray()); bb.putInt(dataLen)
        for (s in samples) bb.putShort((s * gain * 32767f).toInt().coerceIn(-32768, 32767).toShort())
        FileOutputStream(file).use { it.write(bb.array()) }
    }

    const val MUSIC_RATE = 22_050
}
