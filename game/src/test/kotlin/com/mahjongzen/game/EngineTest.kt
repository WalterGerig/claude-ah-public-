package com.mahjongzen.game

import com.mahjongzen.game.engine.GameEngine
import com.mahjongzen.game.engine.GameState
import com.mahjongzen.game.engine.Seeds
import com.mahjongzen.game.gen.Generator
import com.mahjongzen.game.layout.LayoutType
import com.mahjongzen.game.model.Difficulty
import com.mahjongzen.game.score.Scoring
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class EngineTest {

    private fun newGame(type: LayoutType = LayoutType.TURTLE, d: Difficulty = Difficulty.MEDIUM, seed: Long = 5): Pair<GameEngine, GameState> {
        val engine = GameEngine.create(type, d, seed)
        return engine to GameEngine.newState(type, d, seed, layout = engine.layout)
    }

    private fun solutionOf(engine: GameEngine, state: GameState) =
        Generator.generate(engine.geometry, state.difficulty, state.seed).solution

    @Test
    fun `playing the solution wins the game`() {
        val (engine, start) = newGame()
        var s = start
        var t = 0L
        for ((a, b) in solutionOf(engine, start)) {
            t += 5_000
            s = engine.match(s, a, b, t)!!.state
        }
        assertTrue(s.won)
        assertEquals(0, s.remaining)
        assertTrue(s.stars in 1..3)
        assertTrue(s.timeBonus >= 0)
    }

    @Test
    fun `illegal match is rejected`() {
        val (engine, s) = newGame()
        val free = engine.freeMask(s)
        val blocked = free.indices.first { !free[it] }
        val other = s.faces.indices.first { it != blocked && s.faces[it] == s.faces[blocked] }
        assertNull(engine.match(s, blocked, other))
        assertNull(engine.match(s, blocked, blocked))
    }

    @Test
    fun `undo restores tiles score and combo, multi level`() {
        val (engine, start) = newGame()
        val sol = solutionOf(engine, start)
        val s1 = engine.match(start, sol[0].first, sol[0].second, 1000)!!.state
        val s2 = engine.match(s1, sol[1].first, sol[1].second, 2000)!!.state
        assertEquals(2, s2.combo)
        val u1 = engine.undo(s2)!!
        assertEquals(s1.faces, u1.faces)
        assertEquals(s1.combo, u1.combo)
        assertEquals(s1.score - Scoring.UNDO_PENALTY, u1.score)
        val u2 = engine.undo(u1)!!
        assertEquals(start.faces, u2.faces)
        assertEquals(0, u2.score)
        assertEquals(2, u2.undosUsed)
        assertNull(engine.undo(u2))
    }

    @Test
    fun `undo reverts a shuffle`() {
        val (engine, start) = newGame()
        val sh = engine.shuffle(start)
        assertTrue(sh.solvable)
        val u = engine.undo(sh.state)!!
        assertEquals(start.faces, u.faces)
    }

    @Test
    fun `combo multiplies points`() {
        val (engine, start) = newGame(d = Difficulty.EASY)
        val sol = solutionOf(engine, start)
        val r1 = engine.match(start, sol[0].first, sol[0].second, 1000)!!
        val r2 = engine.match(r1.state, sol[1].first, sol[1].second, 2500)!!
        val r3 = engine.match(r2.state, sol[2].first, sol[2].second, 10_000)!!
        assertEquals(1, r1.combo); assertEquals(2, r2.combo); assertEquals(1, r3.combo)
        assertEquals(2, r3.state.maxCombo)
    }

    @Test
    fun `hints are limited on hard`() {
        val (engine, start) = newGame(d = Difficulty.HARD)
        var s = start
        repeat(5) { s = engine.hint(s)!!.state }
        assertEquals(0, engine.hintsRemaining(s))
        assertNull(engine.hint(s))
        assertEquals(0, s.score)
        val (e2, easy) = newGame(d = Difficulty.EASY)
        assertNull(e2.hintsRemaining(easy))
    }

    @Test
    fun `hint returns a removable pair`() {
        val (engine, s) = newGame()
        val h = engine.hint(s)!!
        assertTrue(engine.canMatch(s, h.pair.first, h.pair.second))
    }

    @Test
    fun `state survives json round trip`() {
        val (engine, start) = newGame(type = LayoutType.PROCEDURAL, seed = 31337)
        val sol = solutionOf(engine, start)
        val s = engine.match(start, sol[0].first, sol[0].second, 1000)!!.state
        val json = Json.encodeToString(GameState.serializer(), s)
        val back = Json.decodeFromString(GameState.serializer(), json)
        assertEquals(s, back)
        val engine2 = GameEngine.forState(back)
        assertEquals(engine.layout.slots, engine2.layout.slots)
        assertNotNull(engine2.undo(back))
    }

    @Test
    fun `blocked detection`() {
        val (engine, start) = newGame()
        assertFalse(engine.isBlocked(start))
    }

    @Test
    fun `daily challenge is deterministic per date`() {
        val a = Seeds.daily(LocalDate.of(2026, 11, 6))
        val b = Seeds.daily(LocalDate.of(2026, 11, 6))
        val c = Seeds.daily(LocalDate.of(2026, 11, 7))
        assertEquals(a, b)
        assertTrue(a.seed != c.seed)
        assertTrue(a.seed in 1..Seeds.MAX_SEED)
        assertEquals("000 000 042", Seeds.format(42))
        assertEquals(123456789L, Seeds.parse("123 456 789"))
    }
}
