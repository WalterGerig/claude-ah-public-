package com.mahjongzen.data.settings

import com.mahjongzen.game.model.Difficulty

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class TableBackground { FELT, DARK_WOOD, BAMBOO, NIGHT, GRADIENT }
enum class TileStyle { CLASSIC, MODERN, HIGH_CONTRAST }

data class AppSettings(
    val soundOn: Boolean = true,
    val soundVolume: Float = 0.7f,
    val musicOn: Boolean = false,
    val musicVolume: Float = 0.35f,
    val haptics: Boolean = true,
    val background: TableBackground = TableBackground.FELT,
    val tileStyle: TileStyle = TileStyle.CLASSIC,
    val highlightFree: Boolean = true,
    val showTimer: Boolean = true,
    val defaultDifficulty: Difficulty = Difficulty.MEDIUM,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val largeTiles: Boolean = false,
    val reduceMotion: Boolean = false,
)
