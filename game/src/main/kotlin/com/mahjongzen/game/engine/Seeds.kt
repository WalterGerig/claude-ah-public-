package com.mahjongzen.game.engine

import com.mahjongzen.game.layout.LayoutType
import com.mahjongzen.game.model.Difficulty
import java.time.LocalDate
import kotlin.random.Random

data class DailyChallenge(val date: LocalDate, val seed: Long, val layout: LayoutType, val difficulty: Difficulty)

object Seeds {
    const val MAX_SEED = 999_999_999L

    fun random(rng: Random = Random.Default): Long = rng.nextLong(1, MAX_SEED + 1)

    /** Seeds are shown with 9 digits, grouped for readability: 123 456 789. */
    fun format(seed: Long): String = "%09d".format(seed).chunked(3).joinToString(" ")

    fun parse(text: String): Long? = text.filter { it.isDigit() }.toLongOrNull()?.takeIf { it in 1..MAX_SEED }

    /** Same challenge for everyone on a given date. */
    fun daily(date: LocalDate): DailyChallenge {
        val day = date.toEpochDay()
        val seed = (mix(day) and Long.MAX_VALUE) % MAX_SEED + 1
        val layouts = LayoutType.entries
        val layout = layouts[Math.floorMod(day, layouts.size.toLong()).toInt()]
        return DailyChallenge(date, seed, layout, Difficulty.MEDIUM)
    }

    /** SplitMix64 finaliser. */
    private fun mix(v: Long): Long {
        var z = v + -0x61c8864680b583ebL
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }
}
