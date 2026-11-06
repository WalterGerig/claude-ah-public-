package com.mahjongzen.game.engine

import com.mahjongzen.game.layout.LayoutType
import com.mahjongzen.game.model.Difficulty
import kotlinx.serialization.Serializable

@Serializable
enum class HistoryType { MATCH, SHUFFLE }

/** One undoable step. */
@Serializable
data class HistoryEntry(
    val type: HistoryType,
    val a: Int = -1,
    val b: Int = -1,
    val faceA: Int = -1,
    val faceB: Int = -1,
    val prevFaces: List<Int>? = null,
    val prevScore: Int = 0,
    val prevCombo: Int = 0,
    val prevLastMatchAtMs: Long = -1,
)

/**
 * Complete, immutable and serialisable game state. The layout itself is not stored –
 * it is rebuilt deterministically from [layoutType], [difficulty] and [seed].
 */
@Serializable
data class GameState(
    val layoutType: LayoutType,
    val difficulty: Difficulty,
    val seed: Long,
    val daily: Boolean = false,
    /** Face ordinal per slot, -1 = removed. */
    val faces: List<Int>,
    val score: Int = 0,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val lastMatchAtMs: Long = -1,
    val elapsedMs: Long = 0,
    val moves: Int = 0,
    val hintsUsed: Int = 0,
    val shufflesUsed: Int = 0,
    val undosUsed: Int = 0,
    val history: List<HistoryEntry> = emptyList(),
    val timeBonus: Int = 0,
    val won: Boolean = false,
    val stars: Int = 0,
) {
    val tileCount: Int get() = faces.size
    val remaining: Int get() = faces.count { it >= 0 }
    val canUndo: Boolean get() = history.isNotEmpty() && !won
}
