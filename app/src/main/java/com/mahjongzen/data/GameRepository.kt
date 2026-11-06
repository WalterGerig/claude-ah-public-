package com.mahjongzen.data

import com.mahjongzen.data.db.AppDatabase
import com.mahjongzen.data.db.GameRecordEntity
import com.mahjongzen.data.db.LayoutStatRow
import com.mahjongzen.data.db.SavedGameEntity
import com.mahjongzen.game.engine.GameState
import com.mahjongzen.game.layout.LayoutType
import com.mahjongzen.game.model.Difficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.LocalDate

data class OverallStats(
    val played: Int = 0,
    val won: Int = 0,
    val longestCombo: Int = 0,
    val dailyStreak: Int = 0,
    val perLayout: List<LayoutStatRow> = emptyList(),
) {
    val winRate: Int get() = if (played == 0) 0 else won * 100 / played
}

data class SavedGameSummary(
    val layout: LayoutType,
    val difficulty: Difficulty,
    val daily: Boolean,
    val seed: Long,
    val progressPercent: Int,
)

/** Statistics, highscores and the auto-saved game (Room). */
class GameRepository(private val db: AppDatabase) {

    private val json = Json { ignoreUnknownKeys = true }

    // ---------- saved game ----------

    suspend fun save(state: GameState) {
        val removed = state.tileCount - state.remaining
        db.savedGame().put(
            SavedGameEntity(
                json = json.encodeToString(GameState.serializer(), state),
                layout = state.layoutType.name,
                difficulty = state.difficulty.name,
                daily = state.daily,
                seed = state.seed,
                progressPercent = removed * 100 / state.tileCount,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun load(): GameState? = db.savedGame().get()?.let {
        runCatching { json.decodeFromString(GameState.serializer(), it.json) }.getOrNull()
    }

    suspend fun clearSaved() = db.savedGame().clear()

    val savedSummary: Flow<SavedGameSummary?> = db.savedGame().observe().map { e ->
        e?.let {
            runCatching {
                SavedGameSummary(LayoutType.valueOf(it.layout), Difficulty.valueOf(it.difficulty), it.daily, it.seed, it.progressPercent)
            }.getOrNull()
        }
    }

    // ---------- statistics ----------

    suspend fun record(state: GameState, won: Boolean) {
        db.records().insert(
            GameRecordEntity(
                layout = state.layoutType.name,
                difficulty = state.difficulty.name,
                seed = state.seed,
                won = won,
                score = state.score,
                timeMs = state.elapsedMs,
                maxCombo = state.maxCombo,
                stars = if (won) state.stars else 0,
                daily = state.daily,
                epochDay = LocalDate.now().toEpochDay(),
                finishedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun bestScore(layout: LayoutType, difficulty: Difficulty): Int? =
        db.records().bestScore(layout.name, difficulty.name)

    val stats: Flow<OverallStats> = combine(
        db.records().countPlayed(),
        db.records().countWon(),
        db.records().longestCombo(),
        db.records().dailyWinDays(),
        db.records().layoutStats(),
    ) { played, won, combo, days, rows ->
        OverallStats(played, won, combo, streak(days, LocalDate.now().toEpochDay()), rows)
    }

    val dailyWinDays: Flow<List<Long>> = db.records().dailyWinDays()

    suspend fun resetStatistics() = db.records().clear()

    companion object {
        /** Consecutive days (ending today or yesterday) with a won daily challenge. */
        fun streak(daysDesc: List<Long>, today: Long): Int {
            if (daysDesc.isEmpty()) return 0
            var expected = if (daysDesc.first() == today) today else today - 1
            var count = 0
            for (d in daysDesc) {
                if (d == expected) { count++; expected-- } else if (d < expected) break
            }
            return count
        }
    }
}
