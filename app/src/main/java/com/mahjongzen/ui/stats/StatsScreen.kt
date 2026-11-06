package com.mahjongzen.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import com.mahjongzen.data.OverallStats
import com.mahjongzen.data.db.LayoutStatRow
import com.mahjongzen.game.layout.LayoutType
import com.mahjongzen.game.model.Difficulty
import com.mahjongzen.ui.components.difficultyName
import com.mahjongzen.ui.components.formatTime
import com.mahjongzen.ui.components.layoutName
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StatsViewModel(private val container: AppContainer) : ViewModel() {
    val stats: StateFlow<OverallStats> = container.games.stats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OverallStats())

    fun reset() {
        viewModelScope.launch { container.games.resetStatistics() }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory { initializer { StatsViewModel(container) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(container: AppContainer, onBack: () -> Unit) {
    val vm: StatsViewModel = viewModel(factory = StatsViewModel.factory(container))
    val stats by vm.stats.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                FlowRow(
                    Modifier.fillMaxWidth().widthIn(max = 720.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = 3,
                ) {
                    StatCard(stringResource(R.string.stats_played), stats.played.toString(), Modifier.weight(1f))
                    StatCard(stringResource(R.string.stats_won), stats.won.toString(), Modifier.weight(1f))
                    StatCard(stringResource(R.string.stats_win_rate), "${stats.winRate} %", Modifier.weight(1f))
                    StatCard(stringResource(R.string.stats_longest_combo), "×${stats.longestCombo}", Modifier.weight(1f))
                    StatCard(stringResource(R.string.stats_daily_streak), stringResource(R.string.stats_days, stats.dailyStreak), Modifier.weight(1f))
                }
            }
            item {
                Text(
                    stringResource(R.string.stats_per_layout), style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            if (stats.perLayout.isEmpty()) {
                item { Text(stringResource(R.string.stats_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            val sorted = stats.perLayout.sortedWith(
                compareBy<LayoutStatRow>(
                    { r -> LayoutType.entries.indexOfFirst { it.name == r.layout } },
                    { r -> Difficulty.entries.indexOfFirst { it.name == r.difficulty } },
                ),
            )
            items(sorted, key = { it.layout + it.difficulty }) { row -> LayoutRow(row) }
            item {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { confirm = true }) {
                    Icon(Icons.Rounded.DeleteOutline, null)
                    Spacer(Modifier.padding(4.dp))
                    Text(stringResource(R.string.stats_reset))
                }
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.stats_reset_title)) },
            text = { Text(stringResource(R.string.stats_reset_message)) },
            confirmButton = { TextButton(onClick = { vm.reset(); confirm = false }) { Text(stringResource(R.string.stats_reset_confirm)) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun LayoutRow(row: LayoutStatRow) {
    val type = LayoutType.entries.firstOrNull { it.name == row.layout } ?: return
    val diff = Difficulty.entries.firstOrNull { it.name == row.difficulty } ?: return
    Card(Modifier.fillMaxWidth().widthIn(max = 720.dp), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${layoutName(type)} · ${difficultyName(diff)}", style = MaterialTheme.typography.titleSmall)
                Text("${row.wonCount} / ${row.played}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${stringResource(R.string.stats_highscore)}: ${row.bestScore ?: "–"}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${stringResource(R.string.stats_best_time)}: ${row.bestTimeMs?.let { formatTime(it / 1000) } ?: "–"}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
