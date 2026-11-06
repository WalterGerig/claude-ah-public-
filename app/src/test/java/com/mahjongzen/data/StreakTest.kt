package com.mahjongzen.data

import org.junit.Assert.assertEquals
import org.junit.Test

class StreakTest {
    @Test
    fun `streak counts consecutive days ending today or yesterday`() {
        assertEquals(0, GameRepository.streak(emptyList(), 100))
        assertEquals(3, GameRepository.streak(listOf(100, 99, 98, 96), 100))
        assertEquals(2, GameRepository.streak(listOf(99, 98), 100))
        assertEquals(0, GameRepository.streak(listOf(97, 96), 100))
    }
}
