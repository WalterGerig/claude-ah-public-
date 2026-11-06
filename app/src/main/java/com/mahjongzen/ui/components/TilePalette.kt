package com.mahjongzen.ui.components

import androidx.compose.ui.graphics.Color
import com.mahjongzen.data.settings.TileStyle

/** Colours of a tile design. */
data class TilePalette(
    val faceTop: Color,
    val faceBottom: Color,
    val side: Color,
    val sideDark: Color,
    val back: Color?,          // coloured back layer (classic jade back) or null
    val outline: Color,
    val outlineWidth: Float,   // relative to tile width
    val ink: Color,
    val red: Color,
    val green: Color,
    val blue: Color,
    val bamboo: Color,
    val bambooDark: Color,
    val pink: Color,
    val purple: Color,
    val yellow: Color,
    val orange: Color,
    val showIndex: Boolean,
    val shadowAlpha: Float,
    val strokeScale: Float,
)

fun paletteFor(style: TileStyle): TilePalette = when (style) {
    TileStyle.CLASSIC -> TilePalette(
        faceTop = Color(0xFFFFFBF0), faceBottom = Color(0xFFF1E6CC),
        side = Color(0xFFD8C9A6), sideDark = Color(0xFFB9A880), back = Color(0xFF2F6B4F),
        outline = Color(0x33000000), outlineWidth = 0.012f,
        ink = Color(0xFF1C2B4A), red = Color(0xFFB3261E), green = Color(0xFF2E7D4F), blue = Color(0xFF1F4E8C),
        bamboo = Color(0xFF2F8A55), bambooDark = Color(0xFF1B5E37), pink = Color(0xFFD9668A), purple = Color(0xFF7B4FA0),
        yellow = Color(0xFFE0A526), orange = Color(0xFFD9772B),
        showIndex = false, shadowAlpha = 0.28f, strokeScale = 1f,
    )
    TileStyle.MODERN -> TilePalette(
        faceTop = Color(0xFFFFFFFF), faceBottom = Color(0xFFF4F5F2),
        side = Color(0xFFD9DCD8), sideDark = Color(0xFFC3C7C2), back = null,
        outline = Color(0x1F000000), outlineWidth = 0.01f,
        ink = Color(0xFF263238), red = Color(0xFFE0543F), green = Color(0xFF2BA36B), blue = Color(0xFF3A7BD5),
        bamboo = Color(0xFF34A853), bambooDark = Color(0xFF1E7A3A), pink = Color(0xFFF06292), purple = Color(0xFF9575CD),
        yellow = Color(0xFFFFB300), orange = Color(0xFFFF8A3D),
        showIndex = true, shadowAlpha = 0.18f, strokeScale = 0.9f,
    )
    TileStyle.HIGH_CONTRAST -> TilePalette(
        faceTop = Color(0xFFFFFFFF), faceBottom = Color(0xFFFFFFFF),
        side = Color(0xFF424242), sideDark = Color(0xFF212121), back = null,
        outline = Color(0xFF000000), outlineWidth = 0.03f,
        ink = Color(0xFF000000), red = Color(0xFFC40000), green = Color(0xFF006B2E), blue = Color(0xFF0033B3),
        bamboo = Color(0xFF006B2E), bambooDark = Color(0xFF003D19), pink = Color(0xFFC2185B), purple = Color(0xFF5B1FA6),
        yellow = Color(0xFF8A5A00), orange = Color(0xFFB34700),
        showIndex = true, shadowAlpha = 0.35f, strokeScale = 1.15f,
    )
}
