package com.mahjongzen.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameRecordDao {
    @Insert
    suspend fun insert(record: GameRecordEntity)

    @Query("SELECT COUNT(*) FROM game_records")
    fun countPlayed(): Flow<Int>

    @Query("SELECT COUNT(*) FROM game_records WHERE won = 1")
    fun countWon(): Flow<Int>

    @Query("SELECT COALESCE(MAX(maxCombo), 0) FROM game_records")
    fun longestCombo(): Flow<Int>

    @Query(
        """
        SELECT layout, difficulty,
               COUNT(*) AS played,
               SUM(CASE WHEN won = 1 THEN 1 ELSE 0 END) AS wonCount,
               MAX(CASE WHEN won = 1 THEN score END) AS bestScore,
               MIN(CASE WHEN won = 1 THEN timeMs END) AS bestTimeMs
        FROM game_records
        GROUP BY layout, difficulty
        """
    )
    fun layoutStats(): Flow<List<LayoutStatRow>>

    @Query("SELECT MAX(score) FROM game_records WHERE won = 1 AND layout = :layout AND difficulty = :difficulty")
    suspend fun bestScore(layout: String, difficulty: String): Int?

    @Query("SELECT DISTINCT epochDay FROM game_records WHERE daily = 1 AND won = 1 ORDER BY epochDay DESC")
    fun dailyWinDays(): Flow<List<Long>>

    @Query("DELETE FROM game_records")
    suspend fun clear()
}

@Dao
interface SavedGameDao {
    @Query("SELECT * FROM saved_game WHERE id = 0")
    suspend fun get(): SavedGameEntity?

    @Query("SELECT * FROM saved_game WHERE id = 0")
    fun observe(): Flow<SavedGameEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(game: SavedGameEntity)

    @Query("DELETE FROM saved_game")
    suspend fun clear()
}
