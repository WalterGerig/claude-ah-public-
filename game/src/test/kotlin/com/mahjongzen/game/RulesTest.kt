package com.mahjongzen.game

import com.mahjongzen.game.model.Slot
import com.mahjongzen.game.model.TileFace
import com.mahjongzen.game.model.TileFace.*
import com.mahjongzen.game.rules.Rules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesTest {

    @Test
    fun `single tile is free`() {
        val l = TestBoards.layoutOf(Slot(0, 0, 0), Slot(10, 0, 0))
        val f = TestBoards.faces(DOT_1, DOT_1)
        assertTrue(Rules.isFree(l.geometry, f, 0))
    }

    @Test
    fun `tile in the middle of a row is blocked, ends are free`() {
        val l = TestBoards.layoutOf(Slot(0, 0, 0), Slot(2, 0, 0), Slot(4, 0, 0))
        val f = TestBoards.faces(DOT_1, DOT_2, DOT_3)
        val geo = l.geometry
        assertTrue(Rules.isFree(geo, f, 0))
        assertFalse(Rules.isFree(geo, f, 1))
        assertTrue(Rules.isFree(geo, f, 2))
    }

    @Test
    fun `middle tile becomes free when one neighbour is removed`() {
        val l = TestBoards.layoutOf(Slot(0, 0, 0), Slot(2, 0, 0), Slot(4, 0, 0))
        val f = TestBoards.faces(DOT_1, DOT_2, DOT_3)
        f[0] = -1
        assertTrue(Rules.isFree(l.geometry, f, 1))
    }

    @Test
    fun `covered tile is blocked even with free sides`() {
        val l = TestBoards.layoutOf(Slot(0, 0, 0), Slot(0, 0, 1))
        val f = TestBoards.faces(DOT_1, DOT_1)
        val i0 = l.slots.indexOf(Slot(0, 0, 0))
        val i1 = l.slots.indexOf(Slot(0, 0, 1))
        assertFalse(Rules.isFree(l.geometry, f, i0))
        assertTrue(Rules.isFree(l.geometry, f, i1))
    }

    @Test
    fun `half covered tile is blocked`() {
        // tile on layer 1 lies half on two tiles of layer 0
        val l = TestBoards.layoutOf(Slot(0, 0, 0), Slot(2, 0, 0), Slot(1, 0, 1))
        val f = TestBoards.faces(DOT_1, DOT_2, DOT_3)
        val geo = l.geometry
        val left = l.slots.indexOf(Slot(0, 0, 0))
        val right = l.slots.indexOf(Slot(2, 0, 0))
        assertFalse(Rules.isFree(geo, f, left))
        assertFalse(Rules.isFree(geo, f, right))
    }

    @Test
    fun `half vertical offset neighbour blocks a side`() {
        // left neighbour shifted half a tile down still touches the side
        val l = TestBoards.layoutOf(Slot(0, 1, 0), Slot(2, 0, 0), Slot(4, 0, 0))
        val f = TestBoards.faces(DOT_1, DOT_2, DOT_3)
        val mid = l.slots.indexOf(Slot(2, 0, 0))
        assertFalse(Rules.isFree(l.geometry, f, mid))
    }

    @Test
    fun `tiles a full tile apart vertically do not block`() {
        val l = TestBoards.layoutOf(Slot(0, 2, 0), Slot(2, 0, 0), Slot(4, 0, 0))
        val f = TestBoards.faces(DOT_1, DOT_2, DOT_3)
        val mid = l.slots.indexOf(Slot(2, 0, 0))
        assertTrue(Rules.isFree(l.geometry, f, mid))
    }

    @Test
    fun `matching rules`() {
        val f = TestBoards.faces(DOT_5, DOT_5, DOT_6, FLOWER_PLUM, FLOWER_ORCHID, SEASON_SPRING, SEASON_WINTER, DRAGON_RED)
        assertTrue(Rules.matches(f, 0, 1))
        assertFalse(Rules.matches(f, 0, 2))
        assertTrue("flowers match each other", Rules.matches(f, 3, 4))
        assertTrue("seasons match each other", Rules.matches(f, 5, 6))
        assertFalse("flower does not match season", Rules.matches(f, 3, 5))
        assertFalse("a tile never matches itself", Rules.matches(f, 0, 0))
        assertFalse(Rules.matches(f, 7, 0))
    }

    @Test
    fun `full set has 144 tiles in 36 groups of 4`() {
        val set = TileFace.fullSet()
        assertEquals(144, set.size)
        val groups = set.groupBy { it.matchGroup }
        assertEquals(36, groups.size)
        assertTrue(groups.values.all { it.size == 4 })
    }

    @Test
    fun `available pairs counts all combinations`() {
        val l = TestBoards.layoutOf(Slot(0, 0, 0), Slot(4, 0, 0), Slot(8, 0, 0), Slot(12, 0, 0))
        val f = TestBoards.faces(DOT_1, DOT_1, DOT_1, DOT_1)
        assertEquals(6, Rules.countAvailablePairs(l.geometry, f))
        assertEquals(6, Rules.availablePairs(l.geometry, f).size)
    }
}
