package com.mahjongzen.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mahjongzen.R
import com.mahjongzen.game.engine.GameState
import com.mahjongzen.game.engine.Seeds
import com.mahjongzen.ui.components.difficultyName
import com.mahjongzen.ui.components.formatTime
import com.mahjongzen.ui.components.layoutName
import kotlinx.coroutines.delay

@Composable
private fun DialogCard(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.widthIn(max = 420.dp),
        ) {
            Column(
                Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { content() }
        }
    }
}

@Composable
private fun DialogButton(text: String, icon: ImageVector, primary: Boolean = false, onClick: () -> Unit) {
    val mod = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(48.dp)
    if (primary) {
        Button(onClick = onClick, modifier = mod) { Icon(icon, null); Spacer(Modifier.size(8.dp)); Text(text) }
    } else {
        FilledTonalButton(onClick = onClick, modifier = mod) { Icon(icon, null); Spacer(Modifier.size(8.dp)); Text(text) }
    }
}

@Composable
fun PauseDialog(
    game: GameState,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onNewGame: () -> Unit,
    onCopySeed: () -> Unit,
    onMenu: () -> Unit,
) {
    DialogCard(onDismiss = onResume) {
        Text(stringResource(R.string.pause_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "${layoutName(game.layoutType)} · ${difficultyName(game.difficulty)}",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onCopySeed) {
            Icon(Icons.Rounded.ContentCopy, null, Modifier.size(16.dp))
            Spacer(Modifier.size(6.dp))
            Text(stringResource(R.string.game_seed, Seeds.format(game.seed)))
        }
        Spacer(Modifier.height(8.dp))
        DialogButton(stringResource(R.string.pause_resume), Icons.Rounded.PlayArrow, primary = true, onClick = onResume)
        DialogButton(stringResource(R.string.pause_restart), Icons.Rounded.Refresh, onClick = onRestart)
        DialogButton(stringResource(R.string.pause_new_game), Icons.Rounded.SkipNext, onClick = onNewGame)
        DialogButton(stringResource(R.string.pause_main_menu), Icons.Rounded.Home, onClick = onMenu)
    }
}

@Composable
fun BlockedDialog(canUndo: Boolean, onShuffle: () -> Unit, onUndo: () -> Unit, onRestart: () -> Unit, onDismiss: () -> Unit) {
    DialogCard(onDismiss = onDismiss) {
        Text(stringResource(R.string.blocked_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.blocked_message), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        DialogButton(stringResource(R.string.blocked_shuffle), Icons.Rounded.Shuffle, primary = true, onClick = onShuffle)
        if (canUndo) DialogButton(stringResource(R.string.blocked_undo), Icons.AutoMirrored.Rounded.Undo, onClick = onUndo)
        DialogButton(stringResource(R.string.blocked_restart), Icons.Rounded.Refresh, onClick = onRestart)
    }
}

@Composable
fun WinDialog(game: GameState, newRecord: Boolean, onNext: () -> Unit, onReplay: () -> Unit, onMenu: () -> Unit) {
    DialogCard(onDismiss = {}) {
        Text(stringResource(R.string.win_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        val starsDesc = stringResource(R.string.win_stars, game.stars)
        Row(Modifier.semantics { contentDescription = starsDesc }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (k in 1..3) AnimatedStar(earned = k <= game.stars, delayMs = 250L * k)
        }
        if (newRecord) {
            Spacer(Modifier.height(8.dp))
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(50)) {
                Text(
                    stringResource(R.string.win_new_record), Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        ResultRow(stringResource(R.string.win_time), formatTime(game.elapsedMs / 1000))
        ResultRow(stringResource(R.string.win_combo), "×${game.maxCombo}")
        ResultRow(stringResource(R.string.win_time_bonus), "+${game.timeBonus}")
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        ResultRow(stringResource(R.string.win_score), game.score.toString(), big = true)
        Spacer(Modifier.height(16.dp))
        DialogButton(stringResource(R.string.win_next), Icons.Rounded.SkipNext, primary = true, onClick = onNext)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onReplay, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.win_replay)) }
            OutlinedButton(onClick = onMenu, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.win_menu)) }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String, big: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = if (big) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge)
        Text(value, style = if (big) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AnimatedStar(earned: Boolean, delayMs: Long) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    Icon(
        Icons.Rounded.Star, contentDescription = null,
        tint = if (earned) Color(0xFFE2B03B) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        modifier = Modifier.size(52.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value },
    )
}
