package com.mahjongzen.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One finished (won or abandoned) game. */
@Entity(tableName = "game_records")
data class GameRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val layout: String,
    val difficulty: String,
    val seed: Long,
    val won: Boolean,
    val score: Int,
    val timeMs: Long,
    val maxCombo: Int,
    val stars: Int,
    val daily: Boolean,
    val epochDay: Long,
    val finishedAt: Long,
)

/** The single auto-saved game in progress (id is always 0). */
@Entity(tableName = "saved_game")
data class SavedGameEntity(
    @PrimaryKey val id: Int = 0,
    val json: String,
    val layout: String,
    val difficulty: String,
    val daily: Boolean,
    val seed: Long,
    val progressPercent: Int,
    val updatedAt: Long,
)

/** Aggregated statistics per layout & difficulty. */
data class LayoutStatRow(
    val layout: String,
    val difficulty: String,
    val played: Int,
    val wonCount: Int,
    val bestScore: Int?,
    val bestTimeMs: Long?,
)
