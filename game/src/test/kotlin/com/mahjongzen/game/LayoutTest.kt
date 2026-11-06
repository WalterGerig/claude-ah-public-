package com.mahjongzen.game

import com.mahjongzen.game.layout.LayoutType
import com.mahjongzen.game.model.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutTest {

    @Test
    fun `expert turtle is the classic 144 tile layout with 5 layers`() {
        val l = LayoutType.TURTLE.build(Difficulty.EXPERT)
        assertEquals(144, l.size)
        assertEquals(5, l.layers)
    }

    @Test
    fun `all layouts are valid for all difficulties`() {
        for (type in LayoutType.entries) for (d in Difficulty.entries) for (seed in 1L..(if (type == LayoutType.PROCEDURAL) 40L else 1L)) {
            val l = type.build(d, seed)
            val name = "$type/$d/$seed"
            assertTrue("$name even (${l.size})", l.size % 2 == 0)
            assertTrue("$name <=144 (${l.size})", l.size <= 144)
            assertTrue("$name >= 40 (${l.size})", l.size >= 40)
            assertFalse("$name no overlap", l.geometry.hasSameLayerOverlap())
            l.slots.forEachIndexed { i, s ->
                if (s.z > 0) assertTrue("$name slot $s supported", l.geometry.below[i].isNotEmpty())
            }
        }
    }

    @Test
    fun `difficulty grows the board`() {
        for (type in LayoutType.entries) {
            val easy = type.build(Difficulty.EASY, 7).size
            val expert = type.build(Difficulty.EXPERT, 7).size
            assertTrue("$type easy=$easy expert=$expert", easy < expert)
        }
    }

    @Test
    fun `procedural layouts are deterministic and symmetric`() {
        val a = LayoutType.PROCEDURAL.build(Difficulty.HARD, 1234)
        val b = LayoutType.PROCEDURAL.build(Difficulty.HARD, 1234)
        assertEquals(a.slots, b.slots)
        val maxX = a.slots.maxOf { it.x }
        val set = a.slots.toSet()
        for (s in a.slots) assertTrue("mirror of $s", set.contains(s.copy(x = maxX - s.x)))
    }

    @Test
    fun `print layout sizes`() {
        for (type in LayoutType.entries) {
            println(type.name + ": " + Difficulty.entries.joinToString { d -> val l = type.build(d, 99); "$d=${l.size}/${l.layers}L" })
        }
    }
}
