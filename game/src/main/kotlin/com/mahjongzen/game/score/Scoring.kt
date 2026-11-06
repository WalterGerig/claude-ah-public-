package com.mahjongzen.game.score

import com.mahjongzen.game.model.Difficulty
import com.mahjongzen.game.model.Suit
import com.mahjongzen.game.model.TileFace

/** All score rules in one place – pure functions, easy to test. */
object Scoring {
    const val BASE_POINTS = 10
    const val COMBO_WINDOW_MS = 3_000L
    const val MAX_MULTIPLIER = 5

    const val HINT_PENALTY = 20
    const val SHUFFLE_PENALTY = 50
    const val UNDO_PENALTY = 10

    fun rareBonus(face: TileFace): Int = when (face.suit) {
        Suit.WIND -> 5
        Suit.DRAGON -> 10
        Suit.FLOWER, Suit.SEASON -> 15
        else -> 0
    }

    /** Combo counter after a match at [nowMs]; resets to 1 if the last match is too long ago. */
    fun nextCombo(previousCombo: Int, lastMatchAtMs: Long, nowMs: Long): Int =
        if (lastMatchAtMs >= 0 && nowMs - lastMatchAtMs <= COMBO_WINDOW_MS) previousCombo + 1 else 1

    fun multiplier(combo: Int): Int = combo.coerceIn(1, MAX_MULTIPLIER)

    /** Points for one removed pair. */
    fun pairPoints(face: TileFace, combo: Int, difficulty: Difficulty): Int =
        (BASE_POINTS + rareBonus(face)) * multiplier(combo) * difficulty.scorePercent / 100

    fun parSeconds(difficulty: Difficulty, tileCount: Int): Int =
        maxOf(60, difficulty.parSecondsFor144 * tileCount / 144)

    /** Bonus for finishing below par time. */
    fun timeBonus(difficulty: Difficulty, tileCount: Int, elapsedMs: Long): Int {
        val remaining = parSeconds(difficulty, tileCount) - (elapsedMs / 1000).toInt()
        return maxOf(0, remaining) * difficulty.timeBonusPerSecond
    }

    /** 1–3 stars depending on time and help used. */
    fun stars(difficulty: Difficulty, tileCount: Int, elapsedMs: Long, hints: Int, shuffles: Int, undos: Int): Int {
        val par = parSeconds(difficulty, tileCount) * 1000L
        return when {
            elapsedMs <= par && shuffles == 0 && hints + undos <= 2 -> 3
            elapsedMs <= par * 3 / 2 && shuffles <= 1 && hints + undos <= 8 -> 2
            else -> 1
        }
    }
}
