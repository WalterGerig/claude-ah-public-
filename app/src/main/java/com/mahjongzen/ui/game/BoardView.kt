package com.mahjongzen.ui.game

import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.mahjongzen.R
import com.mahjongzen.data.settings.AppSettings
import com.mahjongzen.game.model.Layout
import com.mahjongzen.game.model.TileFace
import com.mahjongzen.ui.components.TileFaceCache
import com.mahjongzen.ui.components.paletteFor
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Pixel geometry of the board inside the view (before user zoom). */
class BoardMetrics(
    val tileW: Float,
    val tileH: Float,
    val depth: Float,
    private val ox: Float,
    private val oy: Float,
    val boardW: Float,
    val boardH: Float,
) {
    fun left(x: Int, z: Int) = ox + x * tileW / 2f - z * depth
    fun top(y: Int, z: Int) = oy + y * tileH / 2f - z * depth
    fun unitX(u: Float) = ox + u * tileW / 2f
    fun unitY(u: Float) = oy + u * tileH / 2f

    companion object {
        const val ASPECT = 1.32f
        const val DEPTH = 0.1f

        fun compute(layout: Layout, viewW: Float, viewH: Float, padding: Float, minTileW: Float): BoardMetrics {
            val extra = DEPTH * (layout.layers - 1) + DEPTH * 1.8f
            val byW = (viewW - 2 * padding) / (layout.width / 2f + extra)
            val byH = (viewH - 2 * padding) / (layout.height * ASPECT / 2f + extra)
            val tw = max(min(byW, byH), minTileW).coerceAtLeast(8f)
            val th = tw * ASPECT
            val d = tw * DEPTH
            val boardW = layout.width * tw / 2f + extra * tw
            val boardH = layout.height * th / 2f + extra * tw
            val ox = (viewW - boardW) / 2f + (layout.layers - 1) * d
            val oy = (viewH - boardH) / 2f + (layout.layers - 1) * d
            return BoardMetrics(tw, th, d, ox, oy, boardW, boardH)
        }
    }
}

@Composable
fun BoardView(
    layout: Layout,
    faces: List<Int>,
    free: BooleanArray,
    selected: Int,
    hint: Pair<Int, Int>?,
    settings: AppSettings,
    effects: BoardEffects,
    windLetters: List<String>,
    faceNames: (TileFace) -> String,
    onTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = remember(settings.tileStyle) { paletteFor(settings.tileStyle) }
    val density = LocalDensity.current
    val pairsDesc = stringResource(R.string.board_description, faces.count { it >= 0 }, countPairs(faces, free))

    BoxWithConstraints(modifier.fillMaxSize().semantics { contentDescription = pairsDesc }) {
        val viewW = constraints.maxWidth.toFloat()
        val viewH = constraints.maxHeight.toFloat()
        val padding = with(density) { 12.dp.toPx() }
        val minTile = if (settings.largeTiles) with(density) { 44.dp.toPx() } else 0f
        val metrics = remember(layout, viewW, viewH, minTile) { BoardMetrics.compute(layout, viewW, viewH, padding, minTile) }
        // Faces are rendered at 2x so they stay crisp when zooming in.
        val cache = remember(palette, metrics.tileW, windLetters) {
            TileFaceCache(palette, (metrics.tileW * 2f).roundToInt(), (metrics.tileH * 2f).roundToInt(), windLetters)
        }

        var zoom by remember(layout) { mutableFloatStateOf(1f) }
        var pan by remember(layout) { mutableStateOf(Offset.Zero) }
        var frame by remember { mutableLongStateOf(System.nanoTime()) }
        val center = Offset(viewW / 2f, viewH / 2f)

        val currentFaces by rememberUpdatedState(faces)
        val currentOnTap by rememberUpdatedState(onTap)

        fun clampPan(p: Offset, z: Float): Offset {
            val mx = max(0f, (metrics.boardW * z - viewW) / 2f + padding)
            val my = max(0f, (metrics.boardH * z - viewH) / 2f + padding)
            return Offset(p.x.coerceIn(-mx, mx), p.y.coerceIn(-my, my))
        }

        fun toBoard(p: Offset): Offset = (p - center - pan) / zoom + center

        fun hitTest(screen: Offset): Int {
            val p = toBoard(screen)
            val f = currentFaces
            for (i in layout.slots.indices.reversed()) {
                if (f[i] < 0) continue
                val s = layout.slots[i]
                val l = metrics.left(s.x, s.z)
                val t = metrics.top(s.y, s.z)
                if (p.x >= l && p.x <= l + metrics.tileW && p.y >= t && p.y <= t + metrics.tileH) return i
            }
            return -1
        }

        // Frame loop – only runs while an animation is visible.
        LaunchedEffect(effects.version, hint) {
            while (true) {
                val t = withFrameNanos { it }
                frame = t
                if (!effects.isActive(t) && hint == null) break
            }
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(layout, metrics) {
                    detectTapGestures(
                        onTap = { pos -> hitTest(pos).takeIf { it >= 0 }?.let { currentOnTap(it) } },
                        onDoubleTap = { zoom = 1f; pan = Offset.Zero },
                    )
                }
                .pointerInput(layout, metrics) {
                    detectTransformGestures { centroid, panChange, zoomChange, _ ->
                        val newZoom = (zoom * zoomChange).coerceIn(1f, 3.5f)
                        val anchored = centroid - center - (centroid - center - pan) * (newZoom / zoom)
                        zoom = newZoom
                        pan = clampPan(anchored + panChange, newZoom)
                    }
                },
        ) {
            val now = frame
            withTransform({
                translate(pan.x, pan.y)
                scale(zoom, zoom, pivot = center)
            }) {
                drawBoard(layout, faces, free, selected, hint, settings.highlightFree, metrics, cache, effects, now)
            }
        }

        // Accessibility: with TalkBack, expose every free tile as a focusable, clickable element.
        val context = LocalContext.current
        val touchExploration = remember(context) {
            (context.getSystemService(AccessibilityManager::class.java))?.isTouchExplorationEnabled == true
        }
        if (touchExploration) {
            for (i in layout.slots.indices) {
                if (faces[i] < 0 || !free[i]) continue
                val s = layout.slots[i]
                val tl = (Offset(metrics.left(s.x, s.z), metrics.top(s.y, s.z)) - center) * zoom + center + pan
                val name = stringResource(R.string.tile_free, faceNames(TileFace.of(faces[i])))
                Box(
                    Modifier
                        .offset { IntOffset(tl.x.roundToInt(), tl.y.roundToInt()) }
                        .size(with(density) { (metrics.tileW * zoom).toDp() }, with(density) { (metrics.tileH * zoom).toDp() })
                        .semantics { contentDescription = name }
                        .clickable { onTap(i) },
                )
            }
        }
    }
}

private fun countPairs(faces: List<Int>, free: BooleanArray): Int {
    val counts = IntArray(TileFace.GROUP_COUNT)
    for (i in faces.indices) if (faces[i] >= 0 && i < free.size && free[i]) counts[TileFace.of(faces[i]).matchGroup]++
    return counts.sumOf { it * (it - 1) / 2 }
}

private fun DrawScope.drawBoard(
    layout: Layout,
    faces: List<Int>,
    free: BooleanArray,
    selected: Int,
    hint: Pair<Int, Int>?,
    highlightFree: Boolean,
    m: BoardMetrics,
    cache: TileFaceCache,
    fx: BoardEffects,
    now: Long,
) {
    val pal = cache.palette
    val tw = m.tileW
    val th = m.tileH
    val d = m.depth
    val cr = CornerRadius(tw * 0.13f)
    val tileSize = Size(tw, th)
    val dst = IntSize(tw.roundToInt(), th.roundToInt())
    val hintPulse = (0.55f + 0.45f * sin(now / 1e9 * 2 * PI * 1.4).toFloat())
    val liftProgress = if (fx.selectedTile == selected) ((now - fx.selectAt) / BoardEffects.LIFT_NANOS.toFloat()).coerceIn(0f, 1f) else 1f

    for (i in layout.slots.indices) {
        val f = faces[i]
        if (f < 0) continue
        val s = layout.slots[i]
        var left = m.left(s.x, s.z)
        var top = m.top(s.y, s.z)
        val isSel = i == selected
        if (isSel) {
            val lift = d * 0.7f * liftProgress
            left -= lift; top -= lift
        }
        if (i == fx.invalidTile && now - fx.invalidAt < BoardEffects.SHAKE_NANOS) {
            val p = (now - fx.invalidAt) / BoardEffects.SHAKE_NANOS.toFloat()
            left += sin(p * PI * 6).toFloat() * tw * 0.06f * (1f - p)
        }
        // soft shadow, coloured back layer, ivory side, face
        drawRoundRect(Color.Black.copy(alpha = pal.shadowAlpha * (if (isSel) 0.9f else 0.55f)), Offset(left + d * (if (isSel) 2.2f else 1.5f), top + d * (if (isSel) 2.4f else 1.7f)), tileSize, cr)
        pal.back?.let { drawRoundRect(it, Offset(left + d, top + d * 1.05f), tileSize, cr) }
        drawRoundRect(pal.sideDark, Offset(left + d * 0.7f, top + d * 0.75f), tileSize, cr)
        drawRoundRect(pal.side, Offset(left + d * 0.38f, top + d * 0.42f), tileSize, cr)
        drawImage(cache[TileFace.of(f)], dstOffset = IntOffset(left.roundToInt(), top.roundToInt()), dstSize = dst)

        val isFree = i < free.size && free[i]
        if (highlightFree && !isFree) {
            drawRoundRect(Color(0xFF1A1408).copy(alpha = 0.26f), Offset(left, top), tileSize, cr)
        }
        if (isSel) {
            drawRoundRect(Color(0xFFE2B03B), Offset(left, top), tileSize, cr, style = Stroke(tw * 0.07f))
            drawRoundRect(Color(0x33FFD66B), Offset(left, top), tileSize, cr)
        } else if (hint != null && (i == hint.first || i == hint.second)) {
            drawRoundRect(Color(0xFF3FC1C9).copy(alpha = hintPulse), Offset(left, top), tileSize, cr, style = Stroke(tw * 0.075f))
        }
    }

    // flying (removed) tiles
    for (ft in fx.flying) {
        val p = ((now - ft.start) / BoardEffects.FLY_NANOS.toFloat()).coerceIn(0f, 1f)
        if (p >= 1f) continue
        val a = layout.slots[ft.slot]
        val b = layout.slots[ft.partner]
        val ax = m.left(a.x, a.z); val ay = m.top(a.y, a.z)
        val bx = m.left(b.x, b.z); val by = m.top(b.y, b.z)
        val ease = 1f - (1f - p) * (1f - p)
        val x = ax + ((ax + bx) / 2f - ax) * ease * 0.6f
        val y = ay + ((ay + by) / 2f - ay) * ease * 0.6f - th * 0.5f * ease
        val scale = 1f + 0.12f * sin(p * PI).toFloat() - 0.25f * p
        withTransform({ scale(scale, scale, pivot = Offset(x + tw / 2, y + th / 2)) }) {
            drawImage(
                cache[TileFace.of(ft.face)], dstOffset = IntOffset(x.roundToInt(), y.roundToInt()), dstSize = dst,
                alpha = (1f - p).coerceIn(0f, 1f),
            )
        }
    }

    // particles
    for (pt in fx.particles) {
        val t = (now - pt.start) / 1e9f
        val life = pt.life / 1e9f
        if (t > life) continue
        val ux = pt.x0 + pt.vx * t
        val uy = pt.y0 + pt.vy * t + 0.5f * BoardEffects.GRAVITY * t * t
        val alpha = (1f - t / life).coerceIn(0f, 1f)
        drawCircle(pt.color.copy(alpha = alpha), pt.size * tw * (0.6f + alpha * 0.4f), Offset(m.unitX(ux), m.unitY(uy)))
    }
}
