package com.mahjongzen.game

import com.mahjongzen.game.model.Difficulty
import com.mahjongzen.game.model.TileFace
import com.mahjongzen.game.score.Scoring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringTest {

    @Test
    fun `combo increases within window and resets after`() {
        assertEquals(1, Scoring.nextCombo(0, -1, 1000))
        assertEquals(2, Scoring.nextCombo(1, 1000, 3000))
        assertEquals(3, Scoring.nextCombo(2, 3000, 6000))
        assertEquals(1, Scoring.nextCombo(3, 6000, 9001))
    }

    @Test
    fun `pair points include rare bonus multiplier and difficulty`() {
        assertEquals(10, Scoring.pairPoints(TileFace.DOT_3, 1, Difficulty.EASY))
        assertEquals(20, Scoring.pairPoints(TileFace.DOT_3, 2, Difficulty.EASY))
        assertEquals(40, Scoring.pairPoints(TileFace.DRAGON_RED, 2, Difficulty.EASY))
        assertEquals(25, Scoring.pairPoints(TileFace.FLOWER_PLUM, 1, Difficulty.EASY))
        assertEquals(15, Scoring.pairPoints(TileFace.WIND_EAST, 1, Difficulty.EASY))
        // multiplier is capped
        assertEquals(50, Scoring.pairPoints(TileFace.DOT_3, 9, Difficulty.EASY))
        assertEquals(20, Scoring.pairPoints(TileFace.DOT_3, 1, Difficulty.EXPERT))
    }

    @Test
    fun `time bonus decreases with time and is never negative`() {
        val fast = Scoring.timeBonus(Difficulty.MEDIUM, 144, 60_000)
        val slow = Scoring.timeBonus(Difficulty.MEDIUM, 144, 500_000)
        assertTrue(fast > slow)
        assertEquals(0, Scoring.timeBonus(Difficulty.MEDIUM, 144, 10_000_000))
        assertEquals((600 - 60) * 3, fast)
    }

    @Test
    fun `stars`() {
        assertEquals(3, Scoring.stars(Difficulty.MEDIUM, 144, 300_000, 0, 0, 1))
        assertEquals(2, Scoring.stars(Difficulty.MEDIUM, 144, 300_000, 0, 1, 0))
        assertEquals(2, Scoring.stars(Difficulty.MEDIUM, 144, 800_000, 0, 0, 0))
        assertEquals(1, Scoring.stars(Difficulty.MEDIUM, 144, 2_000_000, 5, 3, 9))
    }
}
