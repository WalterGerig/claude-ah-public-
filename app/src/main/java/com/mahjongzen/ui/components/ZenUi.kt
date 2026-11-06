package com.mahjongzen.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mahjongzen.R

/**
 * The app's own calm design language for menus and dialogs: dark felt green, a single warm
 * ivory accent, two type sizes, no icons per row, no heavy shadows or gradients.
 */
object Zen {
    val Background = Color(0xFF10201A)
    val Card = Color(0xFF182C24)
    val CardPressed = Color(0xFF1E352B)
    val Divider = Color(0x14FFFFFF)
    val Track = Color(0xFF263D33)
    val Text = Color(0xFFEDE6D6)
    val TextMuted = Color(0xFF93A69B)
    val Label = Color(0xFF7F978A)
    val Accent = Color(0xFFE3CF9E)
    val OnAccent = Color(0xFF1B2A22)
    val Danger = Color(0xFFD08C80)

    val Body = TextStyle(fontSize = 16.sp, color = Text)
    val Small = TextStyle(fontSize = 13.sp, color = TextMuted, lineHeight = 18.sp)
    val Heading = TextStyle(fontSize = 16.sp, color = Text, fontWeight = FontWeight.Medium)
    val SectionLabel = TextStyle(fontSize = 13.sp, color = Label, fontWeight = FontWeight.Medium, letterSpacing = 1.6.sp)

    val CardShape = RoundedCornerShape(16.dp)
}

/** Full-screen page with a simple header (back arrow + title) on the felt background. */
@Composable
fun ZenPage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Zen.Background).systemBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = Zen.Text) }
                Text(title, style = Zen.Heading)
            }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp), content = content)
            }
        }
    }
}

/** Group: small spaced uppercase label + one card. */
@Composable
fun ZenSection(label: String, footer: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Spacer(Modifier.height(24.dp))
    Text(label.uppercase(), style = Zen.SectionLabel, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
    Column(Modifier.fillMaxWidth().clip(Zen.CardShape).background(Zen.Card), content = content)
    if (footer != null) Text(footer, style = Zen.Small, modifier = Modifier.padding(start = 4.dp, top = 8.dp, end = 4.dp))
}

@Composable
fun ZenDivider() {
    Box(Modifier.fillMaxWidth().padding(start = 18.dp).height(1.dp).background(Zen.Divider))
}

/** Title (and optional muted subtitle) on the left, control on the right. */
@Composable
fun ZenRow(
    title: String,
    subtitle: String? = null,
    titleColor: Color = Zen.Text,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Zen.Body.copy(color = titleColor))
            if (subtitle != null) Text(subtitle, style = Zen.Small)
        }
        if (trailing != null) {
            Spacer(Modifier.size(12.dp))
            trailing()
        }
    }
}

/** A row with the title above a full-width control (segmented control, previews, slider). */
@Composable
fun ZenBlock(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)) {
        Text(title, style = Zen.Body)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
fun ZenSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Zen.OnAccent,
            checkedTrackColor = Zen.Accent,
            checkedBorderColor = Zen.Accent,
            uncheckedThumbColor = Zen.TextMuted,
            uncheckedTrackColor = Zen.Track,
            uncheckedBorderColor = Zen.Track,
        ),
    )
}

/** Slider that only persists the value when the finger is lifted. */
@Composable
fun ZenSlider(value: Float, enabled: Boolean, onChange: (Float) -> Unit) {
    var local by remember(value) { mutableFloatStateOf(value) }
    Slider(
        value = local,
        onValueChange = { local = it },
        onValueChangeFinished = { onChange(local) },
        enabled = enabled,
        colors = SliderDefaults.colors(
            thumbColor = Zen.Accent,
            activeTrackColor = Zen.Accent,
            inactiveTrackColor = Zen.Track,
            disabledThumbColor = Zen.TextMuted,
            disabledActiveTrackColor = Zen.Track,
            disabledInactiveTrackColor = Zen.Track,
        ),
    )
}

@Composable
fun <T> ZenSegmented(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Zen.Track).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        for (o in options) {
            val isSel = o == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSel) Zen.Accent else Color.Transparent)
                    .selectable(selected = isSel, role = Role.RadioButton, onClick = { onSelect(o) })
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label(o), style = Zen.Small.copy(color = if (isSel) Zen.OnAccent else Zen.Text), maxLines = 1, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Small selectable preview (table or tile design) with a fine frame when selected. */
@Composable
fun ZenPreviewChoice(label: String, selected: Boolean, onClick: () -> Unit, preview: @Composable () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(width = 76.dp, height = 56.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(
                    if (selected) BorderStroke(1.5.dp, Zen.Accent) else BorderStroke(1.dp, Zen.Divider),
                    RoundedCornerShape(10.dp),
                ),
            contentAlignment = Alignment.Center,
        ) { preview() }
        Spacer(Modifier.height(6.dp))
        Text(label, style = Zen.Small.copy(color = if (selected) Zen.Text else Zen.TextMuted), maxLines = 1)
    }
}

// ------------------------------------------------------------------ dialogs & buttons

@Composable
fun ZenDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = Zen.CardShape, color = Zen.Card, modifier = Modifier.widthIn(max = 400.dp)) {
            Column(
                Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
        }
    }
}

@Composable
fun ZenPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Zen.Accent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = Zen.Heading.copy(color = Zen.OnAccent)) }
}

@Composable
fun ZenSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth(), color: Color = Zen.Text) {
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = Zen.Body.copy(color = color)) }
}

@Composable
fun ZenConfirmDialog(
    title: String,
    message: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    ZenDialog(onDismiss) {
        Text(title, style = Zen.Heading, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(message, style = Zen.Small, textAlign = TextAlign.Center)
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ZenSecondaryButton(stringResource(R.string.cancel), onDismiss, Modifier.weight(1f))
            ZenSecondaryButton(confirm, onConfirm, Modifier.weight(1f), color = if (destructive) Zen.Danger else Zen.Accent)
        }
    }
}
