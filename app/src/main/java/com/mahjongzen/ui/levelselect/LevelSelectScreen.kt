package com.mahjongzen.ui.levelselect

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mahjongzen.AppContainer
import com.mahjongzen.R
import com.mahjongzen.data.settings.AppSettings
import com.mahjongzen.game.engine.Seeds
import com.mahjongzen.game.layout.LayoutType
import com.mahjongzen.game.model.Difficulty
import com.mahjongzen.ui.components.LayoutPreview
import com.mahjongzen.ui.components.difficultyName
import com.mahjongzen.ui.components.layoutName
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class LevelSelectViewModel(container: AppContainer) : ViewModel() {
    /** Highscore per (layout, difficulty). */
    val best: StateFlow<Map<Pair<String, String>, Int?>> = container.games.stats
        .map { s -> s.perLayout.associate { (it.layout to it.difficulty) to it.bestScore } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory { initializer { LevelSelectViewModel(container) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelSelectScreen(
    container: AppContainer,
    settings: AppSettings,
    onBack: () -> Unit,
    onStart: (LayoutType, Difficulty, Long) -> Unit,
) {
    val vm: LevelSelectViewModel = viewModel(factory = LevelSelectViewModel.factory(container))
    val best by vm.best.collectAsStateWithLifecycle()
    var difficulty by rememberSaveable { mutableStateOf(settings.defaultDifficulty) }
    var layout by rememberSaveable { mutableStateOf(LayoutType.TURTLE) }
    var seedText by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.level_select_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) } },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = { onStart(layout, difficulty, Seeds.parse(seedText) ?: 0L) },
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(52.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.padding(4.dp))
                    Text(stringResource(R.string.level_start) + " · " + layoutName(layout) + " · " + difficultyName(difficulty))
                }
            }
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(stringResource(R.string.level_difficulty), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        Difficulty.entries.forEachIndexed { i, d ->
                            SegmentedButton(
                                selected = d == difficulty,
                                onClick = { difficulty = d },
                                shape = SegmentedButtonDefaults.itemShape(i, Difficulty.entries.size),
                            ) { Text(difficultyName(d), maxLines = 1) }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        difficulty.maxHints?.let { stringResource(R.string.level_hints_limited, it) } ?: stringResource(R.string.level_hints_unlimited),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = seedText,
                        onValueChange = { v -> seedText = v.filter { it.isDigit() }.take(9) },
                        label = { Text(stringResource(R.string.level_seed_label)) },
                        supportingText = { Text(stringResource(R.string.level_seed_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.level_layout), style = MaterialTheme.typography.titleMedium)
                }
            }
            items(LayoutType.entries.toList(), key = { it.name }) { type ->
                LayoutCard(
                    type = type,
                    difficulty = difficulty,
                    selected = type == layout,
                    best = best[type.name to difficulty.name],
                    onClick = { layout = type },
                )
            }
        }
    }
}

@Composable
private fun LayoutCard(type: LayoutType, difficulty: Difficulty, selected: Boolean, best: Int?, onClick: () -> Unit) {
    val preview = remember(type, difficulty) { type.build(difficulty, 1L) }
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        border = if (selected) BorderStroke(2.dp, colors.primary) else null,
        colors = CardDefaults.cardColors(containerColor = if (selected) colors.primaryContainer else colors.surfaceContainer),
    ) {
        Column(Modifier.padding(12.dp)) {
            Surface(color = androidx.compose.ui.graphics.Color(0xFF235C41), shape = RoundedCornerShape(12.dp)) {
                LayoutPreview(
                    preview, androidx.compose.ui.graphics.Color(0xFFFBF5E6), androidx.compose.ui.graphics.Color(0xFFB9A880),
                    Modifier.fillMaxWidth().aspectRatio(1.4f).padding(8.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(layoutName(type), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                if (type == LayoutType.PROCEDURAL) stringResource(R.string.layout_procedural_desc)
                else stringResource(R.string.level_tiles, preview.size, preview.layers),
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
            )
            Text(
                best?.let { stringResource(R.string.level_best_score, it) } ?: stringResource(R.string.level_no_score),
                style = MaterialTheme.typography.bodySmall, color = colors.secondary,
            )
        }
    }
}
