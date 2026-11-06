package com.mahjongzen.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mahjongzen.data.settings.ThemeMode

// Calm, noble palette: jade, brass, rice paper.
private val Jade = Color(0xFF2E6B4F)
private val JadeLight = Color(0xFF8FD0AE)
private val Brass = Color(0xFF8E6E32)
private val BrassLight = Color(0xFFD9B872)
private val Cinnabar = Color(0xFFB3261E)

private val LightColors = lightColorScheme(
    primary = Jade,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDE9D9),
    onPrimaryContainer = Color(0xFF0B2E1E),
    secondary = Brass,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1E2C0),
    onSecondaryContainer = Color(0xFF2E2208),
    tertiary = Cinnabar,
    background = Color(0xFFF6F1E7),
    onBackground = Color(0xFF1E1C17),
    surface = Color(0xFFFBF8F1),
    onSurface = Color(0xFF1E1C17),
    surfaceVariant = Color(0xFFECE5D6),
    onSurfaceVariant = Color(0xFF4D4739),
    surfaceContainer = Color(0xFFF2ECDF),
    surfaceContainerHigh = Color(0xFFEDE6D7),
    outline = Color(0xFF7F7867),
)

private val DarkColors = darkColorScheme(
    primary = JadeLight,
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF1F5139),
    onPrimaryContainer = Color(0xFFCDE9D9),
    secondary = BrassLight,
    onSecondary = Color(0xFF3D2E04),
    secondaryContainer = Color(0xFF574419),
    onSecondaryContainer = Color(0xFFF1E2C0),
    tertiary = Color(0xFFFFB4A9),
    background = Color(0xFF111512),
    onBackground = Color(0xFFE3E3DC),
    surface = Color(0xFF161B17),
    onSurface = Color(0xFFE3E3DC),
    surfaceVariant = Color(0xFF2C332E),
    onSurfaceVariant = Color(0xFFC1C9C1),
    surfaceContainer = Color(0xFF1C221E),
    surfaceContainerHigh = Color(0xFF242B26),
    outline = Color(0xFF8B938B),
)

private val Base = Typography()
private val AppTypography = Base.copy(
    displaySmall = Base.displaySmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium),
    headlineMedium = Base.headlineMedium.copy(fontFamily = FontFamily.Serif),
    headlineSmall = Base.headlineSmall.copy(fontFamily = FontFamily.Serif),
    titleLarge = Base.titleLarge.copy(fontFamily = FontFamily.Serif),
)

val TitleStyle = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 44.sp, letterSpacing = 1.sp)

@Composable
fun MahjongTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = AppTypography, content = content)
}
