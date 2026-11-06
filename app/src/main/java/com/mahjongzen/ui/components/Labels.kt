package com.mahjongzen.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mahjongzen.R
import com.mahjongzen.data.settings.TableBackground
import com.mahjongzen.data.settings.ThemeMode
import com.mahjongzen.data.settings.TileStyle
import com.mahjongzen.game.layout.LayoutType
import com.mahjongzen.game.model.Difficulty
import com.mahjongzen.game.model.Suit
import com.mahjongzen.game.model.TileFace

@Composable
fun layoutName(type: LayoutType): String = stringResource(
    when (type) {
        LayoutType.TURTLE -> R.string.layout_turtle
        LayoutType.PYRAMID -> R.string.layout_pyramid
        LayoutType.FORTRESS -> R.string.layout_fortress
        LayoutType.DRAGON -> R.string.layout_dragon
        LayoutType.BRIDGE -> R.string.layout_bridge
        LayoutType.SPIDER -> R.string.layout_spider
        LayoutType.PROCEDURAL -> R.string.layout_procedural
    }
)

@Composable
fun difficultyName(d: Difficulty): String = stringResource(
    when (d) {
        Difficulty.EASY -> R.string.difficulty_easy
        Difficulty.MEDIUM -> R.string.difficulty_medium
        Difficulty.HARD -> R.string.difficulty_hard
        Difficulty.EXPERT -> R.string.difficulty_expert
    }
)

@Composable
fun backgroundName(b: TableBackground): String = stringResource(
    when (b) {
        TableBackground.FELT -> R.string.bg_felt
        TableBackground.DARK_WOOD -> R.string.bg_dark_wood
        TableBackground.BAMBOO -> R.string.bg_bamboo
        TableBackground.NIGHT -> R.string.bg_night
        TableBackground.GRADIENT -> R.string.bg_gradient
    }
)

@Composable
fun tileStyleName(s: TileStyle): String = stringResource(
    when (s) {
        TileStyle.CLASSIC -> R.string.style_classic
        TileStyle.MODERN -> R.string.style_modern
        TileStyle.HIGH_CONTRAST -> R.string.style_high_contrast
    }
)

@Composable
fun themeName(t: ThemeMode): String = stringResource(
    when (t) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    }
)

/** Spoken name of a tile (TalkBack). */
@Composable
fun faceName(face: TileFace): String = when (face.suit) {
    Suit.DOTS -> stringResource(R.string.face_dot, face.rank)
    Suit.BAMBOO -> stringResource(R.string.face_bamboo, face.rank)
    Suit.CHARACTERS -> stringResource(R.string.face_char, face.rank)
    Suit.WIND -> stringResource(listOf(R.string.wind_east, R.string.wind_south, R.string.wind_west, R.string.wind_north)[face.rank - 1])
    Suit.DRAGON -> stringResource(listOf(R.string.dragon_red, R.string.dragon_green, R.string.dragon_white)[face.rank - 1])
    Suit.FLOWER -> stringResource(listOf(R.string.flower_plum, R.string.flower_orchid, R.string.flower_chrysanthemum, R.string.flower_bamboo)[face.rank - 1])
    Suit.SEASON -> stringResource(listOf(R.string.season_spring, R.string.season_summer, R.string.season_autumn, R.string.season_winter)[face.rank - 1])
}

@Composable
fun windLetters(): List<String> = listOf(
    stringResource(R.string.wind_letter_east),
    stringResource(R.string.wind_letter_south),
    stringResource(R.string.wind_letter_west),
    stringResource(R.string.wind_letter_north),
)

fun formatTime(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds / 60) % 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

enum class Haptic { TICK, CONFIRM, REJECT, HEAVY }

fun View.haptic(kind: Haptic, enabled: Boolean) {
    if (!enabled) return
    val constant = when (kind) {
        Haptic.TICK -> HapticFeedbackConstants.CLOCK_TICK
        Haptic.CONFIRM -> if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
        Haptic.REJECT -> if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
        Haptic.HEAVY -> HapticFeedbackConstants.LONG_PRESS
    }
    performHapticFeedback(constant)
}
