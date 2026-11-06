package com.mahjongzen.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.mahjongzen.ui.components.backgroundName
import com.mahjongzen.ui.components.difficultyName
import com.mahjongzen.ui.components.themeName
import com.mahjongzen.ui.components.tileStyleName
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(container: AppContainer, settings: AppSettings, onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    var confirmReset by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Section(stringResource(R.string.section_sound))
                SwitchRow(stringResource(R.string.sound_effects), null, settings.soundOn) { v -> vm.update { setSoundOn(v) } }
                SliderRow(stringResource(R.string.sound_volume), settings.soundVolume, settings.soundOn) { v -> vm.update { setSoundVolume(v) } }
                SwitchRow(stringResource(R.string.music), null, settings.musicOn) { v -> vm.update { setMusicOn(v) } }
                SliderRow(stringResource(R.string.music_volume), settings.musicVolume, settings.musicOn) { v -> vm.update { setMusicVolume(v) } }
                Text(stringResource(R.string.sound_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Section(stringResource(R.string.section_gameplay))
                SwitchRow(stringResource(R.string.haptics), null, settings.haptics) { v -> vm.update { setHaptics(v) } }
                SwitchRow(stringResource(R.string.highlight_free), stringResource(R.string.highlight_free_desc), settings.highlightFree) { v -> vm.update { setHighlightFree(v) } }
                SwitchRow(stringResource(R.string.show_timer), null, settings.showTimer) { v -> vm.update { setShowTimer(v) } }
                ChoiceRow(stringResource(R.string.default_difficulty), Difficulty.entries, settings.defaultDifficulty, { difficultyName(it) }) { v -> vm.update { setDefaultDifficulty(v) } }

                Section(stringResource(R.string.section_appearance))
                ChoiceRow(stringResource(R.string.theme), ThemeMode.entries, settings.themeMode, { themeName(it) }) { v -> vm.update { setThemeMode(v) } }
                ChoiceRow(stringResource(R.string.background), TableBackground.entries, settings.background, { backgroundName(it) }) { v -> vm.update { setBackground(v) } }
                ChoiceRow(stringResource(R.string.tile_style), TileStyle.entries, settings.tileStyle, { tileStyleName(it) }) { v -> vm.update { setTileStyle(v) } }
                SwitchRow(stringResource(R.string.large_tiles), stringResource(R.string.large_tiles_desc), settings.largeTiles) { v -> vm.update { setLargeTiles(v) } }

                Section(stringResource(R.string.section_language))
                val current = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').substringBefore(',')
                val languages = listOf("" to stringResource(R.string.language_system), "de" to "Deutsch", "en" to "English")
                ChoiceRow(null, languages.map { it.first }, current, { tag -> languages.first { it.first == tag }.second }) { tag ->
                    AppCompatDelegate.setApplicationLocales(
                        if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
                    )
                }

                Section(stringResource(R.string.section_data))
                OutlinedButton(onClick = { confirmReset = true }) { Text(stringResource(R.string.stats_reset)) }
                Text(
                    stringResource(R.string.about), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.stats_reset_title)) },
            text = { Text(stringResource(R.string.stats_reset_message)) },
            confirmButton = { TextButton(onClick = { vm.resetStatistics(); confirmReset = false }) { Text(stringResource(R.string.stats_reset_confirm)) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun Section(title: String) {
    HorizontalDivider(Modifier.padding(top = 16.dp, bottom = 8.dp))
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun SwitchRow(title: String, description: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(title: String, value: Float, enabled: Boolean, onChange: (Float) -> Unit) {
    var local by remember(value) { mutableStateOf(value) }
    Column(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = local, onValueChange = { local = it }, onValueChangeFinished = { onChange(local) }, enabled = enabled)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceRow(title: String?, options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        if (title != null) Text(title, style = MaterialTheme.typography.bodyLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (o in options) {
                FilterChip(selected = o == selected, onClick = { onSelect(o) }, label = { Text(label(o)) })
            }
        }
    }
}
