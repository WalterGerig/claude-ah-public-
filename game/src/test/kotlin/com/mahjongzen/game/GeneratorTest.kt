package com.mahjongzen.game

import com.mahjongzen.game.gen.Generator
import com.mahjongzen.game.gen.Solver
import com.mahjongzen.game.layout.LayoutType
import com.mahjongzen.game.model.Difficulty
import com.mahjongzen.game.model.TileFace
import com.mahjongzen.game.rules.Rules
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratorTest {

    @Test
    fun `1000 seeds - every generated board has a valid solution`() {
        val layouts = LayoutType.entries
        val difficulties = Difficulty.entries
        for (seed in 1L..1000L) {
            val type = layouts[(seed % layouts.size).toInt()]
            val d = difficulties[((seed / layouts.size) % difficulties.size).toInt()]
            val layout = type.build(d, seed)
            val board = Generator.generate(layout.geometry, d, seed)
            assertTrue("$type/$d/$seed all slots filled", board.faces.all { it >= 0 })
            assertTrue("$type/$d/$seed solution valid", Solver.verify(layout.geometry, board.faces, board.solution))
        }
    }

    @Test
    fun `independent solver solves generated boards`() {
        var solved = 0
        val total = 1000
        var limit = 0
        for (seed in 1L..total.toLong()) {
            val type = LayoutType.entries[(seed % LayoutType.entries.size).toInt()]
            val d = Difficulty.entries[((seed / 7) % 4).toInt()]
            val layout = type.build(d, seed)
            val board = Generator.generate(layout.geometry, d, seed)
            val solver = Solver(layout.geometry, nodeLimit = 600_000)
            val solution = solver.solve(board.faces)
            if (solution != null) {
                assertTrue(Solver.verify(layout.geometry, board.faces, solution))
                solved++
            } else {
                // The solver may only give up because of its search limit, never because it proved unsolvable.
                assertTrue("$type/$d/$seed proven unsolvable!", solver.limitReached)
                limit++
            }
        }
        println("Solver: $solved/$total solved, $limit hit the node limit")
        assertTrue("solver should solve the vast majority", solved >= total * 0.97)
    }

    @Test
    fun `face distribution uses matching pairs only`() {
        val layout = LayoutType.TURTLE.build(Difficulty.EXPERT)
        val board = Generator.generate(layout.geometry, Difficulty.EXPERT, 42)
        val counts = board.faces.groupBy { TileFace.of(it).matchGroup }.mapValues { it.value.size }
        assertEquals(36, counts.size)
        assertTrue(counts.values.all { it == 4 })
        // flowers: every flower exactly once
        val flowers = board.faces.map { TileFace.of(it) }.filter { it.matchGroup == TileFace.FLOWER_GROUP }
        assertEquals(4, flowers.toSet().size)
    }

    @Test
    fun `same seed gives same board`() {
        val layout = LayoutType.PYRAMID.build(Difficulty.HARD)
        val a = Generator.generate(layout.geometry, Difficulty.HARD, 777)
        val b = Generator.generate(layout.geometry, Difficulty.HARD, 777)
        assertArrayEquals(a.faces, b.faces)
    }

    @Test
    fun `easy boards have more free pairs than expert boards on average`() {
        val layout = LayoutType.TURTLE.build(Difficulty.HARD)
        var easy = 0; var expert = 0
        for (seed in 1L..30L) {
            easy += Rules.countAvailablePairs(layout.geometry, Generator.generate(layout.geometry, Difficulty.EASY, seed).faces)
            expert += Rules.countAvailablePairs(layout.geometry, Generator.generate(layout.geometry, Difficulty.EXPERT, seed).faces)
        }
        assertTrue("easy=$easy expert=$expert", easy > expert)
    }

    @Test
    fun `shuffle keeps the position solvable`() {
        for (seed in 1L..100L) {
            val type = LayoutType.entries[(seed % LayoutType.entries.size).toInt()]
            val layout = type.build(Difficulty.MEDIUM, seed)
            val geo = layout.geometry
            val board = Generator.generate(geo, Difficulty.MEDIUM, seed)
            // play the first third of the known solution
            val faces = board.faces.copyOf()
            for ((a, b) in board.solution.take(board.solution.size / 3)) { faces[a] = -1; faces[b] = -1 }
            val shuffled = Generator.shuffle(geo, faces, seed * 3)
            assertNotNull("$type/$seed shuffle", shuffled)
            shuffled!!
            // same occupied slots, same multiset of faces
            for (i in faces.indices) assertEquals(faces[i] >= 0, shuffled.faces[i] >= 0)
            assertEquals(faces.filter { it >= 0 }.sorted(), shuffled.faces.filter { it >= 0 }.sorted())
            assertTrue(Solver.verify(geo, shuffled.faces, shuffled.solution))
        }
    }
}
