package com.mahjongzen.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mahjongzen.AppContainer
import com.mahjongzen.R
import com.mahjongzen.data.settings.AppSettings
import com.mahjongzen.data.settings.SettingsRepository
import com.mahjongzen.data.settings.TableBackground
import com.mahjongzen.data.settings.ThemeMode
import com.mahjongzen.data.settings.TileStyle
import com.mahjongzen.game.model.Difficulty
import com.mahjongzen.game.model.TileFace
import com.mahjongzen.ui.components.TableTextures
import com.mahjongzen.ui.components.TilePreview
import com.mahjongzen.ui.components.Zen
import com.mahjongzen.ui.components.ZenBlock
import com.mahjongzen.ui.components.ZenConfirmDialog
import com.mahjongzen.ui.components.ZenDivider
import com.mahjongzen.ui.components.ZenPage
import com.mahjongzen.ui.components.ZenPreviewChoice
import com.mahjongzen.ui.components.ZenRow
import com.mahjongzen.ui.components.ZenSection
import com.mahjongzen.ui.components.ZenSegmented
import com.mahjongzen.ui.components.ZenSlider
import com.mahjongzen.ui.components.ZenSwitch
import com.mahjongzen.ui.components.backgroundName
import com.mahjongzen.ui.components.difficultyName
import com.mahjongzen.ui.components.drawTable
import com.mahjongzen.ui.components.paletteFor
import com.mahjongzen.ui.components.themeName
import com.mahjongzen.ui.components.tileStyleName
import com.mahjongzen.ui.components.windLetters
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    private val repo: SettingsRepository = container.settings

    fun update(block: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch { repo.block() }
    }

    fun resetStatistics() {
        viewModelScope.launch { container.games.resetStatistics() }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory { initializer { SettingsViewModel(container) } }
    }
}

@Composable
fun SettingsScreen(container: AppContainer, settings: AppSettings, onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    SettingsContent(settings, vm::update, vm::resetStatistics, onBack)
}

/** Stateless settings page (previewable): [update] persists a change, [onResetStats] clears the statistics. */
@Composable
fun SettingsContent(
    settings: AppSettings,
    update: (suspend SettingsRepository.() -> Unit) -> Unit,
    onResetStats: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmReset by remember { mutableStateOf(false) }
    val textures = remember { TableTextures() }
    val letters = windLetters()

    ZenPage(title = stringResource(R.string.settings_title), onBack = onBack) {
        // ---------------- Spiel ----------------
        ZenSection(stringResource(R.string.section_gameplay)) {
            ZenBlock(stringResource(R.string.default_difficulty)) {
                ZenSegmented(Difficulty.entries, settings.defaultDifficulty, { difficultyName(it) }) { v -> update { setDefaultDifficulty(v) } }
            }
            ZenDivider()
            ZenRow(stringResource(R.string.highlight_free), stringResource(R.string.highlight_free_desc)) {
                ZenSwitch(settings.highlightFree) { v -> update { setHighlightFree(v) } }
            }
            ZenDivider()
            ZenRow(stringResource(R.string.show_timer)) {
                ZenSwitch(settings.showTimer) { v -> update { setShowTimer(v) } }
            }
        }

        // ---------------- Darstellung ----------------
        ZenSection(stringResource(R.string.section_appearance)) {
            ZenBlock(stringResource(R.string.background)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (bg in TableBackground.entries) {
                        ZenPreviewChoice(backgroundName(bg), bg == settings.background, { update { setBackground(bg) } }) {
                            Canvas(Modifier.fillMaxSize()) { drawTable(bg, textures) }
                        }
                    }
                }
            }
            ZenDivider()
            ZenBlock(stringResource(R.string.tile_style)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (style in TileStyle.entries) {
                        val palette = remember(style) { paletteFor(style) }
                        ZenPreviewChoice(tileStyleName(style), style == settings.tileStyle, { update { setTileStyle(style) } }) {
                            Box(Modifier.fillMaxSize().background(Zen.Track)) {
                                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                    TilePreview(TileFace.DRAGON_RED, palette, letters, Modifier.size(width = 30.dp, height = 40.dp))
                                    TilePreview(TileFace.DOT_5, palette, letters, Modifier.size(width = 30.dp, height = 40.dp))
                                }
                            }
                        }
                    }
                }
            }
            ZenDivider()
            ZenBlock(stringResource(R.string.theme)) {
                ZenSegmented(ThemeMode.entries, settings.themeMode, { themeName(it) }) { v -> update { setThemeMode(v) } }
            }
            ZenDivider()
            ZenRow(stringResource(R.string.large_tiles), stringResource(R.string.large_tiles_desc)) {
                ZenSwitch(settings.largeTiles) { v -> update { setLargeTiles(v) } }
            }
            ZenDivider()
            ZenRow(stringResource(R.string.reduce_motion), stringResource(R.string.reduce_motion_desc)) {
                ZenSwitch(settings.reduceMotion) { v -> update { setReduceMotion(v) } }
            }
            ZenDivider()
            ZenBlock(stringResource(R.string.section_language)) {
                val current = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore(',').substringBefore('-')
                val languages = listOf("" to stringResource(R.string.language_system), "de" to "Deutsch", "en" to "English")
                ZenSegmented(languages.map { it.first }, current, { tag -> languages.first { it.first == tag }.second }) { tag ->
                    AppCompatDelegate.setApplicationLocales(
                        if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
                    )
                }
            }
        }

        // ---------------- Ton & Haptik ----------------
        ZenSection(stringResource(R.string.section_sound), footer = stringResource(R.string.sound_note)) {
            ZenRow(stringResource(R.string.sound_effects)) {
                ZenSwitch(settings.soundOn) { v -> update { setSoundOn(v) } }
            }
            ZenBlock(stringResource(R.string.sound_volume)) {
                ZenSlider(settings.soundVolume, settings.soundOn) { v -> update { setSoundVolume(v) } }
            }
            ZenDivider()
            ZenRow(stringResource(R.string.music)) {
                ZenSwitch(settings.musicOn) { v -> update { setMusicOn(v) } }
            }
            ZenBlock(stringResource(R.string.music_volume)) {
                ZenSlider(settings.musicVolume, settings.musicOn) { v -> update { setMusicVolume(v) } }
            }
            ZenDivider()
            ZenRow(stringResource(R.string.haptics)) {
                ZenSwitch(settings.haptics) { v -> update { setHaptics(v) } }
            }
        }

        // ---------------- Daten ----------------
        ZenSection(stringResource(R.string.section_data), footer = stringResource(R.string.about)) {
            ZenRow(stringResource(R.string.stats_reset), titleColor = Zen.Danger, onClick = { confirmReset = true })
        }
    }

    if (confirmReset) {
        ZenConfirmDialog(
            title = stringResource(R.string.stats_reset_title),
            message = stringResource(R.string.stats_reset_message),
            confirm = stringResource(R.string.stats_reset_confirm),
            onConfirm = { onResetStats(); confirmReset = false },
            onDismiss = { confirmReset = false },
            destructive = true,
        )
    }
}
