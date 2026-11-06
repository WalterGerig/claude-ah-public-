package com.mahjongzen.game.engine

import com.mahjongzen.game.gen.Generator
import com.mahjongzen.game.layout.LayoutType
import com.mahjongzen.game.model.Difficulty
import com.mahjongzen.game.model.Layout
import com.mahjongzen.game.model.TileFace
import com.mahjongzen.game.rules.Rules
import com.mahjongzen.game.score.Scoring
import kotlin.random.Random

data class MatchOutcome(val state: GameState, val points: Int, val combo: Int)
data class HintOutcome(val state: GameState, val pair: Pair<Int, Int>)
data class ShuffleOutcome(val state: GameState, val solvable: Boolean)

/**
 * Stateless game logic for one layout. All functions take a [GameState] and return a new one.
 */
class GameEngine(val layout: Layout) {

    val geometry = layout.geometry

    fun isFree(state: GameState, i: Int): Boolean = Rules.isFree(geometry, state.faces.toIntArray(), i)

    fun freeMask(state: GameState): BooleanArray {
        val f = state.faces.toIntArray()
        return BooleanArray(geometry.size) { Rules.isFree(geometry, f, it) }
    }

    fun availablePairs(state: GameState): List<Pair<Int, Int>> = Rules.availablePairs(geometry, state.faces.toIntArray())

    fun countAvailablePairs(state: GameState): Int = Rules.countAvailablePairs(geometry, state.faces.toIntArray())

    fun canMatch(state: GameState, a: Int, b: Int): Boolean = Rules.canRemove(geometry, state.faces.toIntArray(), a, b)

    fun isBlocked(state: GameState): Boolean = !state.won && state.remaining > 0 && countAvailablePairs(state) == 0

    /** Removes the pair (a, b) at game time [nowMs]. Returns null if the move is illegal. */
    fun match(state: GameState, a: Int, b: Int, nowMs: Long = state.elapsedMs): MatchOutcome? {
        if (state.won || !canMatch(state, a, b)) return null
        val combo = Scoring.nextCombo(state.combo, state.lastMatchAtMs, nowMs)
        val face = TileFace.of(state.faces[a])
        val points = Scoring.pairPoints(face, combo, state.difficulty)
        val faces = state.faces.toMutableList().also { it[a] = -1; it[b] = -1 }
        val entry = HistoryEntry(
            type = HistoryType.MATCH, a = a, b = b, faceA = state.faces[a], faceB = state.faces[b],
            prevScore = state.score, prevCombo = state.combo, prevLastMatchAtMs = state.lastMatchAtMs,
        )
        var next = state.copy(
            faces = faces,
            score = state.score + points,
            combo = combo,
            maxCombo = maxOf(state.maxCombo, combo),
            lastMatchAtMs = nowMs,
            elapsedMs = maxOf(state.elapsedMs, nowMs),
            moves = state.moves + 1,
            history = state.history + entry,
        )
        if (next.remaining == 0) next = finish(next)
        return MatchOutcome(next, points, combo)
    }

    private fun finish(state: GameState): GameState {
        val bonus = Scoring.timeBonus(state.difficulty, state.tileCount, state.elapsedMs)
        val stars = Scoring.stars(state.difficulty, state.tileCount, state.elapsedMs, state.hintsUsed, state.shufflesUsed, state.undosUsed)
        return state.copy(won = true, timeBonus = bonus, score = state.score + bonus, stars = stars)
    }

    /** Remaining hints or null if unlimited. */
    fun hintsRemaining(state: GameState): Int? = state.difficulty.maxHints?.let { maxOf(0, it - state.hintsUsed) }

    /** Returns a removable pair and charges the hint penalty, or null. */
    fun hint(state: GameState): HintOutcome? {
        if (state.won) return null
        if (hintsRemaining(state) == 0) return null
        val pairs = availablePairs(state)
        if (pairs.isEmpty()) return null
        // Prefer a pair whose group is completely free (always a safe move).
        val f = state.faces
        val free = freeMask(state)
        val pair = pairs.firstOrNull { (a, _) ->
            val g = TileFace.of(f[a]).matchGroup
            val remainingInGroup = f.indices.count { f[it] >= 0 && TileFace.of(f[it]).matchGroup == g }
            val freeInGroup = f.indices.count { free[it] && TileFace.of(f[it]).matchGroup == g }
            remainingInGroup == freeInGroup
        } ?: pairs.first()
        val next = state.copy(score = maxOf(0, state.score - Scoring.HINT_PENALTY), hintsUsed = state.hintsUsed + 1)
        return HintOutcome(next, pair)
    }

    /** Re-deals the remaining tiles, keeping the position solvable when possible. */
    fun shuffle(state: GameState): ShuffleOutcome {
        val faces = state.faces.toIntArray()
        val seed = state.seed * 31 + state.shufflesUsed * 7919L + state.moves * 104_729L
        val generated = Generator.shuffle(geometry, faces, seed)
        val newFaces: List<Int>
        val solvable: Boolean
        if (generated != null) {
            newFaces = generated.faces.toList()
            solvable = true
        } else {
            val positions = faces.indices.filter { faces[it] >= 0 }
            val values = positions.map { faces[it] }.shuffled(Random(seed))
            val arr = IntArray(faces.size) { -1 }
            positions.forEachIndexed { k, p -> arr[p] = values[k] }
            newFaces = arr.toList()
            solvable = false
        }
        val entry = HistoryEntry(
            type = HistoryType.SHUFFLE, prevFaces = state.faces,
            prevScore = state.score, prevCombo = state.combo, prevLastMatchAtMs = state.lastMatchAtMs,
        )
        val next = state.copy(
            faces = newFaces,
            score = maxOf(0, state.score - Scoring.SHUFFLE_PENALTY),
            combo = 0,
            shufflesUsed = state.shufflesUsed + 1,
            history = state.history + entry,
        )
        return ShuffleOutcome(next, solvable)
    }

    /** Undoes the last step (match or shuffle). Returns null if there is nothing to undo. */
    fun undo(state: GameState): GameState? {
        if (!state.canUndo) return null
        val entry = state.history.last()
        val history = state.history.dropLast(1)
        return when (entry.type) {
            HistoryType.MATCH -> {
                val faces = state.faces.toMutableList().also { it[entry.a] = entry.faceA; it[entry.b] = entry.faceB }
                state.copy(
                    faces = faces,
                    score = maxOf(0, entry.prevScore - Scoring.UNDO_PENALTY),
                    combo = entry.prevCombo,
                    lastMatchAtMs = entry.prevLastMatchAtMs,
                    undosUsed = state.undosUsed + 1,
                    history = history,
                )
            }
            HistoryType.SHUFFLE -> state.copy(
                faces = entry.prevFaces ?: state.faces,
                score = maxOf(0, state.score - Scoring.UNDO_PENALTY),
                combo = entry.prevCombo,
                lastMatchAtMs = entry.prevLastMatchAtMs,
                undosUsed = state.undosUsed + 1,
                history = history,
            )
        }
    }

    /** Same board from the start (used for "restart"). */
    fun restart(state: GameState): GameState = newState(state.layoutType, state.difficulty, state.seed, state.daily, layout)

    companion object {
        fun create(type: LayoutType, difficulty: Difficulty, seed: Long): GameEngine =
            GameEngine(type.build(difficulty, seed))

        fun forState(state: GameState): GameEngine = create(state.layoutType, state.difficulty, state.seed)

        fun newState(type: LayoutType, difficulty: Difficulty, seed: Long, daily: Boolean = false, layout: Layout = type.build(difficulty, seed)): GameState {
            val board = Generator.generate(layout.geometry, difficulty, seed)
            return GameState(layoutType = type, difficulty = difficulty, seed = seed, daily = daily, faces = board.faces.toList())
        }
    }
}
