package com.mahjongzen.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mahjongzen.AppContainer
import com.mahjongzen.R
import com.mahjongzen.audio.Sfx
import com.mahjongzen.data.settings.AppSettings
import com.mahjongzen.game.engine.Seeds
import com.mahjongzen.game.model.TileFace
import com.mahjongzen.ui.components.Haptic
import com.mahjongzen.ui.components.TableBackgroundView
import com.mahjongzen.ui.components.difficultyName
import com.mahjongzen.ui.components.faceName
import com.mahjongzen.ui.components.formatTime
import com.mahjongzen.ui.components.haptic
import com.mahjongzen.ui.components.layoutName
import com.mahjongzen.ui.components.windLetters
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private val HudColor = Color(0x59000000)
private val HudText = Color(0xFFF7F1E3)

private data class ScorePopup(val id: Int, val points: Int, val combo: Int)

@Composable
fun GameScreen(container: AppContainer, settings: AppSettings, onExit: () -> Unit) {
    val vm: GameViewModel = viewModel(factory = GameViewModel.factory(container))
    val ui by vm.ui.collectAsStateWithLifecycle()
    val view = LocalView.current
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val effects = remember { BoardEffects() }
    var popup by remember { mutableStateOf<ScorePopup?>(null) }
    var confetti by remember { mutableIntStateOf(0) }
    var blockedDismissed by remember { mutableStateOf(false) }

    val msgNoHints = stringResource(R.string.hint_none_left)
    val msgUnsolvable = stringResource(R.string.shuffle_unsolvable)
    val msgCopied = stringResource(R.string.seed_copied)
    val letters = windLetters()
    val names = TileFace.entries.map { faceName(it) }
    val currentLayout by rememberUpdatedState(ui.layout)
    val currentSettings by rememberUpdatedState(settings)

    // pause the timer when the app goes to the background
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> vm.setForeground(true)
                Lifecycle.Event.ON_PAUSE -> vm.setForeground(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(vm) {
        vm.events.collect { e ->
            val now = System.nanoTime()
            val hapticsOn = currentSettings.haptics
            when (e) {
                is GameEvent.Selected -> {
                    container.sound.play(Sfx.SELECT)
                    view.haptic(Haptic.TICK, hapticsOn)
                    effects.onSelect(e.tile, now)
                }
                is GameEvent.Invalid -> {
                    container.sound.play(Sfx.INVALID)
                    view.haptic(Haptic.REJECT, hapticsOn)
                    effects.onInvalid(e.tile, now)
                }
                is GameEvent.Matched -> {
                    val combo = e.combo
                    container.sound.play(if (combo >= 2) Sfx.COMBO else Sfx.MATCH, 1f + (min(combo, 5) - 1) * 0.06f)
                    view.haptic(Haptic.CONFIRM, hapticsOn)
                    currentLayout?.let { l ->
                        val a = l.slots[e.a]
                        val b = l.slots[e.b]
                        effects.onRemoved(
                            e.a, e.b, e.faceA, e.faceB,
                            a.x - a.z * 0.2f, a.y - a.z * 0.15f, b.x - b.z * 0.2f, b.y - b.z * 0.15f, combo, now,
                        )
                    }
                    popup = ScorePopup((popup?.id ?: 0) + 1, e.points, combo)
                    blockedDismissed = false
                }
                is GameEvent.Hint -> container.sound.play(Sfx.HINT)
                GameEvent.NoHintsLeft -> scope.launch { snackbar.showSnackbar(msgNoHints) }
                is GameEvent.Shuffled -> {
                    container.sound.play(Sfx.SHUFFLE)
                    blockedDismissed = false
                    if (!e.solvable) scope.launch { snackbar.showSnackbar(msgUnsolvable) }
                }
                GameEvent.Undone -> {
                    container.sound.play(Sfx.UNDO)
                    blockedDismissed = false
                }
                GameEvent.Won -> {
                    container.sound.play(Sfx.WIN)
                    view.haptic(Haptic.HEAVY, hapticsOn)
                    confetti++
                }
                GameEvent.Blocked -> view.haptic(Haptic.REJECT, hapticsOn)
            }
        }
    }

    BackHandler {
        if (ui.paused) vm.resume() else if (ui.won) onExit() else vm.pause()
    }

    Box(Modifier.fillMaxSize()) {
        TableBackgroundView(settings.background)
        val game = ui.game
        val layout = ui.layout
        if (ui.loading || game == null || layout == null) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = HudText)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.game_loading), color = HudText)
            }
        } else {
            BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding()) {
                val landscape = maxWidth > maxHeight && maxHeight < 600.dp
                val board: @Composable (Modifier) -> Unit = { mod ->
                    Box(mod) {
                        BoardView(
                            layout = layout, faces = game.faces, free = ui.free, selected = ui.selected, hint = ui.hint,
                            settings = settings, effects = effects, windLetters = letters,
                            faceNames = { names[it.ordinal] }, onTap = vm::onTileTapped,
                        )
                        ScorePopupView(popup, Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
                    }
                }
                if (landscape) {
                    Row(Modifier.fillMaxSize()) {
                        SidePanel(ui, settings, vm, Modifier.fillMaxHeight().width(150.dp).padding(8.dp))
                        board(Modifier.weight(1f).fillMaxHeight())
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        TopHud(ui, settings, onPause = vm::pause)
                        board(Modifier.weight(1f).fillMaxWidth())
                        BottomBar(ui, vm)
                    }
                }
            }
            if (ui.paused) {
                PauseDialog(
                    game = game,
                    onResume = vm::resume,
                    onRestart = vm::restart,
                    onNewGame = vm::newGame,
                    onCopySeed = {
                        clipboard.setText(AnnotatedString(game.seed.toString()))
                        scope.launch { snackbar.showSnackbar(msgCopied) }
                    },
                    onMenu = { vm.resume(); onExit() },
                )
            }
            if (ui.blocked && !ui.paused && !blockedDismissed) {
                BlockedDialog(
                    canUndo = game.canUndo,
                    onShuffle = vm::shuffle,
                    onUndo = vm::undo,
                    onRestart = vm::restart,
                    onDismiss = { blockedDismissed = true },
                )
            }
            if (game.won) {
                WinDialog(game = game, newRecord = ui.newRecord, onNext = vm::newGame, onReplay = vm::restart, onMenu = onExit)
            }
        }
        Confetti(confetti)
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
    }
}

@Composable
private fun HudStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = HudText.copy(alpha = 0.7f), fontSize = 11.sp, maxLines = 1)
        Text(value, color = HudText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Serif, maxLines = 1)
    }
}

@Composable
private fun TopHud(ui: GameUiState, settings: AppSettings, onPause: () -> Unit) {
    val g = ui.game ?: return
    Surface(color = HudColor, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPause) { Icon(Icons.Rounded.Pause, stringResource(R.string.game_pause), tint = HudText) }
            HudStat(stringResource(R.string.game_score), g.score.toString(), Modifier.weight(1f))
            if (settings.showTimer) HudStat(stringResource(R.string.game_time), formatTime(ui.elapsedSeconds), Modifier.weight(1f))
            HudStat(stringResource(R.string.game_moves), g.moves.toString(), Modifier.weight(1f))
            HudStat(stringResource(R.string.game_pairs), ui.availablePairs.toString(), Modifier.weight(1f))
            HudStat(stringResource(R.string.game_tiles), g.remaining.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, enabled: Boolean, badge: Int?, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick, enabled = enabled, shape = RoundedCornerShape(18.dp),
            color = if (enabled) Color(0xFFF7F1E3) else Color(0x66F7F1E3),
            modifier = Modifier.size(width = 64.dp, height = 48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                BadgedBox(badge = { if (badge != null) Badge { Text(badge.toString()) } }) {
                    Icon(icon, contentDescription = label, tint = Color(0xFF1F4A35))
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(label, color = HudText, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun BottomBar(ui: GameUiState, vm: GameViewModel) {
    val g = ui.game ?: return
    Surface(color = HudColor, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            ActionButton(Icons.Rounded.Lightbulb, stringResource(R.string.game_hint), ui.hintsRemaining != 0 && !g.won, ui.hintsRemaining, vm::hint)
            ActionButton(Icons.Rounded.Shuffle, stringResource(R.string.game_shuffle), !g.won, null, vm::shuffle)
            ActionButton(Icons.AutoMirrored.Rounded.Undo, stringResource(R.string.game_undo), g.canUndo, null, vm::undo)
        }
    }
}

@Composable
private fun SidePanel(ui: GameUiState, settings: AppSettings, vm: GameViewModel, modifier: Modifier) {
    val g = ui.game ?: return
    Surface(color = HudColor, shape = RoundedCornerShape(24.dp), modifier = modifier) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = vm::pause) { Icon(Icons.Rounded.Pause, stringResource(R.string.game_pause), tint = HudText) }
            HudStat(stringResource(R.string.game_score), g.score.toString())
            if (settings.showTimer) HudStat(stringResource(R.string.game_time), formatTime(ui.elapsedSeconds))
            Row {
                HudStat(stringResource(R.string.game_pairs), ui.availablePairs.toString(), Modifier.weight(1f))
                HudStat(stringResource(R.string.game_tiles), g.remaining.toString(), Modifier.weight(1f))
            }
            Text(
                "${layoutName(g.layoutType)} · ${difficultyName(g.difficulty)}\n${if (g.daily) stringResource(R.string.game_daily) else Seeds.format(g.seed)}",
                color = HudText.copy(alpha = 0.6f), fontSize = 10.sp, lineHeight = 13.sp,
            )
            ActionButton(Icons.Rounded.Lightbulb, stringResource(R.string.game_hint), ui.hintsRemaining != 0 && !g.won, ui.hintsRemaining, vm::hint)
            ActionButton(Icons.Rounded.Shuffle, stringResource(R.string.game_shuffle), !g.won, null, vm::shuffle)
            ActionButton(Icons.AutoMirrored.Rounded.Undo, stringResource(R.string.game_undo), g.canUndo, null, vm::undo)
        }
    }
}

@Composable
private fun ScorePopupView(data: ScorePopup?, modifier: Modifier) {
    if (data == null) return
    key(data.id) {
        val anim = remember { Animatable(0f) }
        LaunchedEffect(Unit) { anim.animateTo(1f, tween(1100, easing = LinearOutSlowInEasing)) }
        if (anim.value < 1f) {
            Column(
                modifier.graphicsLayer {
                    translationY = -anim.value * 60.dp.toPx()
                    alpha = 1f - anim.value * anim.value
                    val s = 1f + 0.25f * sin(anim.value * Math.PI).toFloat() * (if (data.combo >= 2) 1f else 0.4f)
                    scaleX = s; scaleY = s
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.game_points, data.points), color = Color(0xFFFFE9A8), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                if (data.combo >= 2) {
                    Text(stringResource(R.string.game_combo, data.combo), color = Color(0xFFFFC857), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private class ConfettiPiece(val x: Float, val delay: Float, val speed: Float, val sway: Float, val phase: Float, val spin: Float, val color: Color, val w: Float)

/** Falling petals / confetti for the victory. */
@Composable
private fun Confetti(trigger: Int) {
    if (trigger == 0) return
    val pieces = remember(trigger) {
        val r = Random(trigger)
        val colors = listOf(Color(0xFFE2B03B), Color(0xFFB3261E), Color(0xFF2E7D4F), Color(0xFFF7F1E3), Color(0xFFD9668A))
        List(150) {
            ConfettiPiece(r.nextFloat(), r.nextFloat() * 1.2f, 0.25f + r.nextFloat() * 0.35f, 1f + r.nextFloat() * 3f, r.nextFloat() * 6.28f, 90f + r.nextFloat() * 360f, colors[r.nextInt(colors.size)], 6f + r.nextFloat() * 8f)
        }
    }
    var t by remember(trigger) { mutableFloatStateOf(0f) }
    LaunchedEffect(trigger) {
        val start = withFrameNanos { it }
        while (t < 5f) withFrameNanos { t = (it - start) / 1e9f }
    }
    if (t >= 5f) return
    Canvas(Modifier.fillMaxSize()) {
        for (p in pieces) {
            val tt = t - p.delay
            if (tt < 0) continue
            val y = -20f + tt * p.speed * size.height
            if (y > size.height + 20f) continue
            val x = p.x * size.width + sin(tt * p.sway + p.phase) * 28f
            val fade = (1f - (t - 4f).coerceAtLeast(0f)).coerceIn(0f, 1f)
            rotate(tt * p.spin + p.phase * 57f, pivot = Offset(x, y)) {
                drawRoundRect(p.color.copy(alpha = fade), Offset(x - p.w / 2, y - p.w * 0.3f), Size(p.w, p.w * 0.6f), CornerRadius(p.w * 0.25f))
            }
        }
    }
}
