package com.mahjongzen.ui.game

import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
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
import com.mahjongzen.ui.components.TableTextures
import com.mahjongzen.ui.components.TileFaceCache
import com.mahjongzen.ui.components.TilePalette
import com.mahjongzen.ui.components.drawWorldTable
import com.mahjongzen.ui.components.paletteFor
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Pixel geometry of the board inside the world (before camera zoom). */
class BoardMetrics(
    val tileW: Float,
    val tileH: Float,
    val depth: Float,
    private val ox: Float,
    private val oy: Float,
    val boardW: Float,
    val boardH: Float,
    private val layoutW: Int,
    private val layoutH: Int,
) {
    fun left(x: Int, z: Int) = ox + x * tileW / 2f - z * depth
    fun top(y: Int, z: Int) = oy + y * tileH / 2f - z * depth
    fun unitX(u: Float) = ox + u * tileW / 2f
    fun unitY(u: Float) = oy + u * tileH / 2f
    val center: Offset get() = Offset(unitX(layoutW / 2f), unitY(layoutH / 2f))

    companion object {
        const val ASPECT = 1.32f
        const val DEPTH = 0.1f

        /** Fits the whole layout into [viewW] × [viewH] minus [padding] on every side, centred. */
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
            return BoardMetrics(tw, th, d, ox, oy, boardW, boardH, layout.width, layout.height)
        }
    }
}

/** Size of the world (floor + table) and the board placed inside it. */
private class World(val width: Float, val height: Float, val margin: Float, val metrics: BoardMetrics)

/**
 * The board. Floor, table and tiles live in ONE world that a camera (scale + translation via
 * graphicsLayer) looks at. Zoom 1x = the whole layout fits on the screen ("fit to screen").
 */
@Composable
fun BoardView(
    layout: Layout,
    faces: List<Int>,
    free: BooleanArray,
    selected: Int,
    hint: Pair<Int, Int>?,
    settings: AppSettings,
    effects: BoardEffects,
    shuffle: ShuffleAnimation?,
    inputEnabled: Boolean,
    windLetters: List<String>,
    faceNames: (TileFace) -> String,
    onTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = remember(settings.tileStyle) { paletteFor(settings.tileStyle) }
    val density = LocalDensity.current
    val textures = remember { TableTextures() }
    val remaining = remember(faces) { faces.count { it >= 0 } }
    val pairCount = remember(faces, free) { countPairs(faces, free) }
    val pairsDesc = stringResource(R.string.board_description, remaining, pairCount)

    BoxWithConstraints(modifier.fillMaxSize().clipToBounds().semantics { contentDescription = pairsDesc }) {
        val viewW = constraints.maxWidth.toFloat()
        val viewH = constraints.maxHeight.toFloat()
        val margin = with(density) { 8.dp.toPx() }
        val inner = with(density) { 14.dp.toPx() } + min(viewW, viewH) * 0.03f // wooden rim + breathing room
        val minTile = if (settings.largeTiles) with(density) { 44.dp.toPx() } else 0f

        val world = remember(layout, viewW, viewH, minTile) {
            val pad = margin + inner
            val fit = BoardMetrics.compute(layout, viewW, viewH, pad, minTile)
            // Normally the world is exactly the view. Only "larger tiles" can make it bigger.
            val w = max(viewW, fit.boardW + 2 * pad)
            val h = max(viewH, fit.boardH + 2 * pad)
            World(w, h, margin, BoardMetrics.compute(layout, w, h, pad, minTile))
        }
        val m = world.metrics
        // Faces are rendered at 2x so they stay crisp when zooming in.
        val cache = remember(palette, m.tileW, windLetters) {
            TileFaceCache(palette, (m.tileW * 2f).roundToInt(), (m.tileH * 2f).roundToInt(), windLetters)
        }

        // ---------------- camera ----------------
        val home = Offset((viewW - world.width) / 2f, (viewH - world.height) / 2f)
        var zoom by remember(world) { mutableFloatStateOf(1f) }
        var offset by remember(world) { mutableStateOf(home) }
        val zoomed by remember(world) { derivedStateOf { zoom > 1.02f || (offset - home).getDistance() > 2f } }
        val scope = rememberCoroutineScope()

        /** Never show anything beyond the world (no panning off the table into the void). */
        fun clamp(o: Offset, z: Float): Offset {
            val w = world.width * z
            val h = world.height * z
            val x = if (w <= viewW) (viewW - w) / 2f else o.x.coerceIn(viewW - w, 0f)
            val y = if (h <= viewH) (viewH - h) / 2f else o.y.coerceIn(viewH - h, 0f)
            return Offset(x, y)
        }

        fun resetView() {
            scope.launch {
                val z0 = zoom
                val o0 = offset
                animate(0f, 1f, animationSpec = tween(350, easing = FastOutSlowInEasing)) { v, _ ->
                    zoom = z0 + (1f - z0) * v
                    offset = clamp(lerp(o0, home, v), zoom)
                }
            }
        }

        val currentFaces by rememberUpdatedState(faces)
        val currentOnTap by rememberUpdatedState(onTap)
        val enabled by rememberUpdatedState(inputEnabled)

        /** Screen position → top-most tile, purely by arithmetic. */
        fun hitTest(screen: Offset): Int {
            val p = (screen - offset) / zoom
            val f = currentFaces
            for (i in layout.slots.indices.reversed()) {
                if (f[i] < 0) continue
                val s = layout.slots[i]
                val l = m.left(s.x, s.z)
                val t = m.top(s.y, s.z)
                if (p.x >= l && p.x <= l + m.tileW && p.y >= t && p.y <= t + m.tileH) return i
            }
            return -1
        }

        // Frame clock – only runs while an animation is visible. Reading `frame` happens in the
        // draw phase only, so animations never cause recomposition.
        var frame by remember { mutableLongStateOf(System.nanoTime()) }
        LaunchedEffect(effects.version, hint, shuffle) {
            while (true) {
                val t = withFrameNanos { it }
                frame = t
                if (!effects.isActive(t) && hint == null && (shuffle == null || shuffle.isDone(t))) break
            }
        }

        // ---------------- the world ----------------
        Box(
            Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .requiredSize(with(density) { world.width.toDp() }, with(density) { world.height.toDp() })
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = zoom
                    scaleY = zoom
                    translationX = offset.x
                    translationY = offset.y
                },
        ) {
            // static layer: floor + table (own render node, never redrawn while tiles animate)
            Canvas(Modifier.fillMaxSize()) { drawWorldTable(settings.background, textures, world.margin) }
            // dynamic layer: tiles and effects
            Canvas(Modifier.fillMaxSize()) {
                drawBoard(layout, faces, free, selected, hint, settings.highlightFree, m, cache, effects, shuffle, frame)
            }
        }

        // gestures over the whole visible area
        Box(
            Modifier
                .matchParentSize()
                .pointerInput(world) {
                    detectBoardGestures(
                        onTap = { pos ->
                            if (enabled) hitTest(pos).takeIf { it >= 0 }?.let { currentOnTap(it) }
                        },
                        onTransform = { centroid, pan, zoomChange ->
                            val nz = (zoom * zoomChange).coerceIn(1f, 3f)
                            // keep the world point under the fingers where it is
                            val worldPoint = (centroid - offset) / zoom
                            offset = clamp(centroid - worldPoint * nz + pan, nz)
                            zoom = nz
                        },
                    )
                },
        )

        // reset to the fit view – only visible when zoomed or moved
        AnimatedVisibility(
            visible = zoomed,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
        ) {
            Surface(onClick = { resetView() }, shape = CircleShape, color = Color(0xCC1B2A22), contentColor = Color(0xFFEDE6D6)) {
                Icon(Icons.Rounded.ZoomOutMap, stringResource(R.string.reset_view), Modifier.padding(10.dp).size(22.dp))
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
                val tl = Offset(m.left(s.x, s.z), m.top(s.y, s.z)) * zoom + offset
                val name = stringResource(R.string.tile_free, faceNames(TileFace.of(faces[i])))
                Box(
                    Modifier
                        .offset { IntOffset(tl.x.roundToInt(), tl.y.roundToInt()) }
                        .size(with(density) { (m.tileW * zoom).toDp() }, with(density) { (m.tileH * zoom).toDp() })
                        .semantics { contentDescription = name }
                        .clickable(enabled = inputEnabled) { onTap(i) },
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

/** One tile: soft shadow, coloured back layer, ivory side and the cached face. */
private fun DrawScope.tileBody(left: Float, top: Float, m: BoardMetrics, pal: TilePalette, face: ImageBitmap, lifted: Boolean = false) {
    val tw = m.tileW
    val th = m.tileH
    val d = m.depth
    val cr = CornerRadius(tw * 0.13f)
    val size = Size(tw, th)
    drawRoundRect(
        Color.Black.copy(alpha = pal.shadowAlpha * (if (lifted) 0.9f else 0.55f)),
        Offset(left + d * (if (lifted) 2.2f else 1.5f), top + d * (if (lifted) 2.4f else 1.7f)), size, cr,
    )
    pal.back?.let { drawRoundRect(it, Offset(left + d, top + d * 1.05f), size, cr) }
    drawRoundRect(pal.sideDark, Offset(left + d * 0.7f, top + d * 0.75f), size, cr)
    drawRoundRect(pal.side, Offset(left + d * 0.38f, top + d * 0.42f), size, cr)
    drawImage(face, dstOffset = IntOffset(left.roundToInt(), top.roundToInt()), dstSize = IntSize(tw.roundToInt(), th.roundToInt()))
}

private fun DrawScope.tileTransformed(x: Float, y: Float, rot: Float, scale: Float, m: BoardMetrics, pal: TilePalette, face: ImageBitmap) {
    val pivot = Offset(x + m.tileW / 2f, y + m.tileH / 2f)
    withTransform({
        rotate(rot, pivot)
        scale(scale, scale, pivot)
    }) { tileBody(x, y, m, pal, face) }
}

private fun easeInQuad(x: Float) = x * x
private fun easeOutBack(x: Float): Float {
    val c1 = 1.2f
    val c3 = c1 + 1f
    val t = x - 1f
    return 1f + c3 * t * t * t + c1 * t * t
}

internal fun DrawScope.drawBoard(
    layout: Layout,
    faces: List<Int>,
    free: BooleanArray,
    selected: Int,
    hint: Pair<Int, Int>?,
    highlightFree: Boolean,
    m: BoardMetrics,
    cache: TileFaceCache,
    fx: BoardEffects,
    shuffle: ShuffleAnimation?,
    now: Long,
) {
    if (shuffle != null && !shuffle.isDone(now) && !shuffle.reduced) {
        drawShuffle(layout, m, cache, shuffle, now)
        return
    }
    // "reduce motion": the old faces simply fade out over the new ones
    val fadeOld = if (shuffle != null && shuffle.reduced && !shuffle.isDone(now)) 1f - shuffle.progress(now) else 0f

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
        tileBody(left, top, m, pal, cache[TileFace.of(f)], lifted = isSel)
        if (fadeOld > 0f) {
            val old = shuffle!!.oldFaces[i]
            if (old >= 0) drawImage(cache[TileFace.of(old)], dstOffset = IntOffset(left.roundToInt(), top.roundToInt()), dstSize = dst, alpha = fadeOld)
        }

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

/**
 * Shuffle: tiles gather in a loose pile (ease-in, staggered), wobble briefly, then fly with the
 * new faces to their places (ease-out with a little overshoot, lower layers first).
 */
private fun DrawScope.drawShuffle(layout: Layout, m: BoardMetrics, cache: TileFaceCache, sh: ShuffleAnimation, now: Long) {
    val pal = cache.palette
    val t = now - sh.start
    val c = m.center
    fun pileX(i: Int) = c.x + sh.pileDx[i] * m.tileW - m.tileW / 2f
    fun pileY(i: Int) = c.y + sh.pileDy[i] * m.tileH - m.tileH / 2f

    if (t < sh.holdEnd) {
        val wobble = if (t > sh.gatherEnd) {
            val p = (t - sh.gatherEnd).toFloat() / ShuffleTiming.HOLD
            sin(p * 2 * PI * 2).toFloat() * 3f * (1f - p)
        } else 0f
        // tiles that have not started yet stay on the board (drawn first, in board order) …
        for (i in layout.slots.indices) {
            val f = sh.oldFaces[i]
            if (f < 0 || t >= sh.gatherRank[i] * sh.stagger) continue
            val s = layout.slots[i]
            tileBody(m.left(s.x, s.z), m.top(s.y, s.z), m, pal, cache[TileFace.of(f)])
        }
        // … the moving / piled ones on top, in the order they arrive
        for (i in sh.byGather) {
            val local = ((t - sh.gatherRank[i] * sh.stagger).toFloat() / ShuffleTiming.GATHER)
            if (local <= 0f) continue
            val e = easeInQuad(local.coerceIn(0f, 1f))
            val s = layout.slots[i]
            val x = m.left(s.x, s.z) + (pileX(i) - m.left(s.x, s.z)) * e
            val y = m.top(s.y, s.z) + (pileY(i) - m.top(s.y, s.z)) * e
            val rot = sh.pileRot[i] * e + wobble * (if (i % 2 == 0) 1f else -1f)
            val scale = 1f - (1f - sh.pileScale[i]) * e
            tileTransformed(x, y, rot, scale, m, pal, cache[TileFace.of(sh.oldFaces[i])])
        }
    } else {
        val ts = t - sh.holdEnd
        // still in the pile (shown with their new faces – the motifs were re-dealt in the pile)
        for (j in sh.byScatter.reversed()) {
            if (ts >= sh.scatterRank[j] * sh.stagger) continue
            val src = sh.source[j]
            tileTransformed(pileX(src), pileY(src), sh.pileRot[src], sh.pileScale[src], m, pal, cache[TileFace.of(sh.newFaces[j])])
        }
        // flying out / landed, lower layers first
        for (j in sh.byScatter) {
            val local = ((ts - sh.scatterRank[j] * sh.stagger).toFloat() / ShuffleTiming.SCATTER)
            if (local < 0f) continue
            val e = easeOutBack(local.coerceIn(0f, 1f))
            val src = sh.source[j]
            val s = layout.slots[j]
            val tx = m.left(s.x, s.z)
            val ty = m.top(s.y, s.z)
            val x = pileX(src) + (tx - pileX(src)) * e
            val y = pileY(src) + (ty - pileY(src)) * e
            val rot = sh.pileRot[src] * (1f - e)
            val scale = sh.pileScale[src] + (1f - sh.pileScale[src]) * e
            tileTransformed(x, y, rot, scale, m, pal, cache[TileFace.of(sh.newFaces[j])])
        }
    }
}
