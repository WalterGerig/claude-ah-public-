package com.mahjongzen.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mahjongzen.R
import com.mahjongzen.game.engine.GameState
import com.mahjongzen.game.engine.Seeds
import com.mahjongzen.ui.components.Zen
import com.mahjongzen.ui.components.ZenDialog
import com.mahjongzen.ui.components.ZenPrimaryButton
import com.mahjongzen.ui.components.ZenSecondaryButton
import com.mahjongzen.ui.components.difficultyName
import com.mahjongzen.ui.components.formatTime
import com.mahjongzen.ui.components.layoutName
import kotlinx.coroutines.delay

private val Gap = Modifier.height(10.dp)

@Composable
fun PauseDialog(
    game: GameState,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onNewGame: () -> Unit,
    onCopySeed: () -> Unit,
    onMenu: () -> Unit,
) {
    ZenDialog(onDismiss = onResume) {
        Text(stringResource(R.string.pause_title), style = Zen.Heading)
        Spacer(Modifier.height(4.dp))
        Text("${layoutName(game.layoutType)} · ${difficultyName(game.difficulty)}", style = Zen.Small)
        Text(
            stringResource(R.string.game_seed, Seeds.format(game.seed)) + "  ·  " + stringResource(R.string.pause_copy_seed),
            style = Zen.Small.copy(color = Zen.Accent),
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onCopySeed).padding(horizontal = 8.dp, vertical = 6.dp),
        )
        Spacer(Modifier.height(16.dp))
        ZenPrimaryButton(stringResource(R.string.pause_resume), onResume)
        Spacer(Gap)
        ZenSecondaryButton(stringResource(R.string.pause_restart), onRestart)
        Spacer(Gap)
        ZenSecondaryButton(stringResource(R.string.pause_new_game), onNewGame)
        Spacer(Gap)
        ZenSecondaryButton(stringResource(R.string.pause_main_menu), onMenu)
    }
}

@Composable
fun BlockedDialog(canUndo: Boolean, onShuffle: () -> Unit, onUndo: () -> Unit, onRestart: () -> Unit, onDismiss: () -> Unit) {
    ZenDialog(onDismiss = onDismiss) {
        Text(stringResource(R.string.blocked_title), style = Zen.Heading)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.blocked_message), style = Zen.Small, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        ZenPrimaryButton(stringResource(R.string.blocked_shuffle), onShuffle)
        if (canUndo) {
            Spacer(Gap)
            ZenSecondaryButton(stringResource(R.string.blocked_undo), onUndo)
        }
        Spacer(Gap)
        ZenSecondaryButton(stringResource(R.string.blocked_restart), onRestart)
    }
}

@Composable
fun WinDialog(game: GameState, newRecord: Boolean, onNext: () -> Unit, onReplay: () -> Unit, onMenu: () -> Unit) {
    ZenDialog(onDismiss = {}) {
        Text(stringResource(R.string.win_title), style = Zen.Heading)
        Spacer(Modifier.height(14.dp))
        val starsDesc = stringResource(R.string.win_stars, game.stars)
        Row(Modifier.semantics { contentDescription = starsDesc }, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (k in 1..3) AnimatedStar(earned = k <= game.stars, delayMs = 250L * k)
        }
        if (newRecord) {
            Spacer(Modifier.height(10.dp))
            Box(Modifier.clip(RoundedCornerShape(50)).background(Zen.Track).padding(horizontal = 14.dp, vertical = 6.dp)) {
                Text(stringResource(R.string.win_new_record), style = Zen.Small.copy(color = Zen.Accent))
            }
        }
        Spacer(Modifier.height(16.dp))
        ResultRow(stringResource(R.string.win_time), formatTime(game.elapsedMs / 1000))
        ResultRow(stringResource(R.string.win_combo), "×${game.maxCombo}")
        ResultRow(stringResource(R.string.win_time_bonus), "+${game.timeBonus}")
        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(Zen.Divider))
        ResultRow(stringResource(R.string.win_score), game.score.toString(), strong = true)
        Spacer(Modifier.height(20.dp))
        ZenPrimaryButton(stringResource(R.string.win_next), onNext)
        Spacer(Gap)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ZenSecondaryButton(stringResource(R.string.win_replay), onReplay, Modifier.weight(1f))
            ZenSecondaryButton(stringResource(R.string.win_menu), onMenu, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String, strong: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = if (strong) Zen.Heading else Zen.Body.copy(color = Zen.TextMuted))
        Text(value, style = if (strong) Zen.Heading.copy(color = Zen.Accent) else Zen.Body)
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
        tint = if (earned) Zen.Accent else Zen.Track,
        modifier = Modifier.size(46.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value },
    )
}
