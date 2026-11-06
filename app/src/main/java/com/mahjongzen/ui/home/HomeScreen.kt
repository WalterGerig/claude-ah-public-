package com.mahjongzen.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mahjongzen.AppContainer
import com.mahjongzen.R
import com.mahjongzen.data.SavedGameSummary
import com.mahjongzen.data.settings.AppSettings
import com.mahjongzen.game.model.TileFace
import com.mahjongzen.ui.components.TableBackgroundView
import com.mahjongzen.ui.components.TilePreview
import com.mahjongzen.ui.components.difficultyName
import com.mahjongzen.ui.components.layoutName
import com.mahjongzen.ui.components.paletteFor
import com.mahjongzen.ui.components.windLetters
import com.mahjongzen.ui.theme.TitleStyle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class HomeViewModel(container: AppContainer) : ViewModel() {
    val saved: StateFlow<SavedGameSummary?> = container.games.savedSummary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val dailyDone: StateFlow<Boolean> = container.games.dailyWinDays
        .map { it.firstOrNull() == LocalDate.now().toEpochDay() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory { initializer { HomeViewModel(container) } }
    }
}

private val Ivory = Color(0xFFF7F1E3)

@Composable
fun HomeScreen(
    container: AppContainer,
    settings: AppSettings,
    onContinue: () -> Unit,
    onPlay: () -> Unit,
    onDaily: () -> Unit,
    onLevelSelect: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
) {
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
    val saved by vm.saved.collectAsStateWithLifecycle()
    val dailyDone by vm.dailyDone.collectAsStateWithLifecycle()
    val palette = remember(settings.tileStyle) { paletteFor(settings.tileStyle) }
    val letters = windLetters()

    Box(Modifier.fillMaxSize()) {
        TableBackgroundView(settings.background)
        BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding()) {
            val landscape = maxWidth > maxHeight
            val header: @Composable () -> Unit = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                        val faces = listOf(TileFace.FLOWER_PLUM, TileFace.DRAGON_RED, TileFace.DOT_1)
                        faces.forEachIndexed { i, f ->
                            TilePreview(
                                f, palette, letters,
                                Modifier.size(width = 62.dp, height = 82.dp).graphicsLayer {
                                    rotationZ = (i - 1) * 8f
                                    translationY = if (i == 1) -10.dp.toPx() else 0f
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(stringResource(R.string.app_name), style = TitleStyle, color = Ivory)
                    Text(stringResource(R.string.home_tagline), color = Ivory.copy(alpha = 0.75f), fontSize = 16.sp)
                }
            }
            val buttons: @Composable () -> Unit = {
                Column(Modifier.widthIn(max = 380.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    saved?.let { s ->
                        MenuButton(
                            stringResource(R.string.home_continue), Icons.Rounded.PlayArrow, primary = true,
                            subtitle = stringResource(R.string.home_continue_detail, layoutName(s.layout), difficultyName(s.difficulty), s.progressPercent),
                            onClick = onContinue,
                        )
                    }
                    MenuButton(stringResource(R.string.home_play), Icons.Rounded.PlayArrow, primary = saved == null, onClick = onPlay)
                    MenuButton(
                        stringResource(if (dailyDone) R.string.home_daily_done else R.string.home_daily), Icons.Rounded.Today,
                        subtitle = LocalDate.now().toString(), onClick = onDaily,
                    )
                    MenuButton(stringResource(R.string.home_level_select), Icons.Rounded.GridView, onClick = onLevelSelect)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MenuButton(stringResource(R.string.home_statistics), Icons.Rounded.BarChart, Modifier.weight(1f), onClick = onStats)
                        MenuButton(stringResource(R.string.home_settings), Icons.Rounded.Settings, Modifier.weight(1f), onClick = onSettings)
                    }
                }
            }
            if (landscape) {
                Row(Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { header() }
                    Box(Modifier.weight(1f).verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) { buttons() }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Spacer(Modifier.height(24.dp))
                    header()
                    Spacer(Modifier.height(36.dp))
                    buttons()
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.about), color = Ivory.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun MenuButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier.fillMaxWidth(),
    primary: Boolean = false,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (primary) Ivory else Color(0x40000000),
        contentColor = if (primary) Color(0xFF1F4A35) else Ivory,
        modifier = modifier,
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = if (subtitle != null) 10.dp else 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.size(14.dp))
            Column {
                Text(text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = if (primary) Color(0xFF4D6B5A) else Ivory.copy(alpha = 0.7f))
            }
        }
    }
}
