package com.mahjongzen.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.mahjongzen.game.model.Suit
import com.mahjongzen.game.model.TileFace
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders every tile face once into an [ImageBitmap] of the requested pixel size.
 * The board then only blits bitmaps, which keeps 144 tiles at 60 fps.
 */
class TileFaceCache(
    val palette: TilePalette,
    val widthPx: Int,
    val heightPx: Int,
    private val windLetters: List<String>,
) {
    private val cache = arrayOfNulls<ImageBitmap>(TileFace.entries.size)

    operator fun get(face: TileFace): ImageBitmap =
        cache[face.ordinal] ?: render(face).also { cache[face.ordinal] = it }

    private fun render(face: TileFace): ImageBitmap {
        val bmp = ImageBitmap(widthPx.coerceAtLeast(8), heightPx.coerceAtLeast(8))
        val canvas = androidx.compose.ui.graphics.Canvas(bmp)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(bmp.width.toFloat(), bmp.height.toFloat())) {
            drawTileFace(face, palette, windLetters)
        }
        return bmp
    }
}

/** Draws the tile's top face (background + motif) filling the whole draw size. */
fun DrawScope.drawTileFace(face: TileFace, pal: TilePalette, windLetters: List<String>) {
    val w = size.width
    val h = size.height
    val r = CornerRadius(w * 0.13f)
    drawRoundRect(Brush.verticalGradient(listOf(pal.faceTop, pal.faceBottom)), cornerRadius = r)
    // soft inner highlight
    drawRoundRect(
        Color.White.copy(alpha = 0.55f), topLeft = Offset(w * 0.03f, h * 0.025f),
        size = Size(w * 0.94f, h * 0.95f), cornerRadius = CornerRadius(w * 0.11f), style = Stroke(w * 0.015f),
    )
    if (pal.outlineWidth > 0f) {
        val ow = w * pal.outlineWidth
        drawRoundRect(pal.outline, topLeft = Offset(ow / 2, ow / 2), size = Size(w - ow, h - ow), cornerRadius = r, style = Stroke(ow))
    }
    val top = if (pal.showIndex && face.suit in setOf(Suit.DOTS, Suit.BAMBOO, Suit.CHARACTERS)) 0.17f else 0.09f
    val area = Area(w * 0.11f, h * top, w * 0.89f, h * 0.92f)
    when (face.suit) {
        Suit.DOTS -> drawDots(face.rank, area, pal)
        Suit.BAMBOO -> drawBamboo(face.rank, area, pal)
        Suit.CHARACTERS -> drawCharacter(face.rank, area, pal)
        Suit.WIND -> drawWind(face.rank, area, pal, windLetters.getOrElse(face.rank - 1) { "" })
        Suit.DRAGON -> drawDragon(face.rank, area, pal)
        Suit.FLOWER -> drawFlower(face.rank, area, pal)
        Suit.SEASON -> drawSeason(face.rank, area, pal)
    }
    if (pal.showIndex && face.suit in setOf(Suit.DOTS, Suit.BAMBOO, Suit.CHARACTERS)) {
        drawLabel(face.rank.toString(), Offset(w * 0.17f, h * 0.15f), w * 0.17f, pal.ink, bold = true)
    }
    if (face.suit == Suit.FLOWER || face.suit == Suit.SEASON) {
        drawLabel(face.rank.toString(), Offset(w * 0.83f, h * 0.17f), w * 0.15f, pal.red, bold = true)
    }
}

/** Motif area in pixels. */
private class Area(val l: Float, val t: Float, val r: Float, val b: Float) {
    val w get() = r - l
    val h get() = b - t
    fun p(x: Float, y: Float) = Offset(l + x * w, t + y * h)
}

private fun DrawScope.drawLabel(text: String, center: Offset, sizePx: Float, color: Color, bold: Boolean = false, serif: Boolean = true) {
    drawIntoCanvas { c ->
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toArgb()
            textSize = sizePx
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(if (serif) Typeface.SERIF else Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
        val fm = paint.fontMetrics
        c.nativeCanvas.drawText(text, center.x, center.y - (fm.ascent + fm.descent) / 2f, paint)
    }
}

// ------------------------------------------------------------------ dots

private fun DrawScope.coin(c: Offset, r: Float, color: Color, pal: TilePalette) {
    drawCircle(color, r, c)
    drawCircle(pal.faceTop, r * 0.74f, c)
    drawCircle(color, r * 0.56f, c)
    drawCircle(pal.faceTop, r * 0.26f, c)
    drawCircle(color.copy(alpha = 0.85f), r * 0.12f, c)
}

private fun DrawScope.drawDots(n: Int, a: Area, pal: TilePalette) {
    val B = pal.blue; val G = pal.green; val R = pal.red
    if (n == 1) {
        val c = a.p(0.5f, 0.5f)
        val rad = a.w * 0.42f
        drawCircle(G, rad, c)
        drawCircle(pal.faceTop, rad * 0.86f, c)
        for (k in 0 until 12) {
            val ang = k * PI / 6
            drawCircle(R, rad * 0.11f, Offset(c.x + cos(ang).toFloat() * rad * 0.7f, c.y + sin(ang).toFloat() * rad * 0.7f))
        }
        drawCircle(B, rad * 0.48f, c)
        drawCircle(pal.faceTop, rad * 0.3f, c)
        drawCircle(R, rad * 0.16f, c)
        return
    }
    data class D(val x: Float, val y: Float, val c: Color)
    val dots: List<D> = when (n) {
        2 -> listOf(D(.5f, .26f, G), D(.5f, .74f, B))
        3 -> listOf(D(.22f, .18f, B), D(.5f, .5f, R), D(.78f, .82f, G))
        4 -> listOf(D(.27f, .25f, B), D(.73f, .25f, G), D(.27f, .75f, G), D(.73f, .75f, B))
        5 -> listOf(D(.25f, .2f, B), D(.75f, .2f, G), D(.5f, .5f, R), D(.25f, .8f, G), D(.75f, .8f, B))
        6 -> listOf(D(.28f, .16f, G), D(.72f, .16f, G), D(.28f, .5f, R), D(.72f, .5f, R), D(.28f, .84f, R), D(.72f, .84f, R))
        7 -> listOf(D(.2f, .12f, G), D(.5f, .24f, G), D(.8f, .36f, G), D(.28f, .62f, R), D(.72f, .62f, R), D(.28f, .87f, R), D(.72f, .87f, R))
        8 -> (0 until 8).map { D(if (it % 2 == 0) .28f else .72f, .12f + (it / 2) * .253f, B) }
        else -> (0 until 9).map { D(.2f + (it % 3) * .3f, .16f + (it / 3) * .34f, listOf(B, R, G)[it / 3]) }
    }
    val r = a.w * when (n) { 2 -> 0.23f; 3 -> 0.19f; 4, 5 -> 0.2f; 6 -> 0.18f; 7 -> 0.155f; 8 -> 0.15f; else -> 0.135f }
    for (d in dots) coin(a.p(d.x, d.y), r, d.c, pal)
}

// ------------------------------------------------------------------ bamboo

private fun DrawScope.stick(c: Offset, sw: Float, sh: Float, color: Color, dark: Color) {
    val tl = Offset(c.x - sw / 2, c.y - sh / 2)
    drawRoundRect(color, tl, Size(sw, sh), CornerRadius(sw / 2))
    drawLine(dark, Offset(tl.x, c.y), Offset(tl.x + sw, c.y), strokeWidth = sh * 0.08f)
    drawLine(dark, Offset(tl.x + sw * 0.15f, tl.y + sh * 0.12f), Offset(tl.x + sw * 0.85f, tl.y + sh * 0.12f), strokeWidth = sh * 0.05f)
    drawLine(dark, Offset(tl.x + sw * 0.15f, tl.y + sh * 0.88f), Offset(tl.x + sw * 0.85f, tl.y + sh * 0.88f), strokeWidth = sh * 0.05f)
    drawLine(Color.White.copy(alpha = 0.45f), Offset(c.x - sw * 0.18f, tl.y + sh * 0.18f), Offset(c.x - sw * 0.18f, tl.y + sh * 0.82f), strokeWidth = sw * 0.16f, cap = StrokeCap.Round)
}

private fun DrawScope.drawBamboo(n: Int, a: Area, pal: TilePalette) {
    val G = pal.bamboo; val D = pal.bambooDark; val R = pal.red
    if (n == 1) {
        drawBird(a, pal)
        return
    }
    data class S(val x: Float, val y: Float, val red: Boolean = false)
    val rows3 = n >= 7 || n == 3 || n == 5 || n == 9
    val sticks: List<S> = when (n) {
        2 -> listOf(S(.5f, .25f), S(.5f, .75f))
        3 -> listOf(S(.5f, .25f), S(.28f, .75f), S(.72f, .75f))
        4 -> listOf(S(.3f, .25f), S(.7f, .25f), S(.3f, .75f), S(.7f, .75f))
        5 -> listOf(S(.22f, .2f), S(.78f, .2f), S(.5f, .5f, true), S(.22f, .8f), S(.78f, .8f))
        6 -> listOf(S(.2f, .25f), S(.5f, .25f), S(.8f, .25f), S(.2f, .75f), S(.5f, .75f), S(.8f, .75f))
        7 -> listOf(S(.5f, .17f, true), S(.2f, .5f), S(.5f, .5f), S(.8f, .5f), S(.2f, .83f), S(.5f, .83f), S(.8f, .83f))
        8 -> listOf(.14f, .38f, .62f, .86f).flatMap { x -> listOf(S(x, .25f), S(x, .75f)) }
        else -> (0 until 9).map { S(.2f + (it % 3) * .3f, .17f + (it / 3) * .33f, it % 3 == 1) }
    }
    val sh = a.h * (if (rows3) 0.29f else 0.42f)
    val sw = a.w * (if (n == 8) 0.17f else 0.2f)
    for (s in sticks) {
        val color = if (s.red) R else G
        stick(a.p(s.x, s.y), sw, sh, color, if (s.red) R.copy(red = R.red * 0.7f) else D)
    }
}

/** The "one of bamboo" is traditionally a bird – here a stylised sparrow on a stalk. */
private fun DrawScope.drawBird(a: Area, pal: TilePalette) {
    val c = a.p(0.5f, 0.5f)
    val u = a.w
    // stalk
    drawLine(pal.bambooDark, a.p(0.3f, 0.95f), a.p(0.42f, 0.62f), strokeWidth = u * 0.07f, cap = StrokeCap.Round)
    // tail feathers
    for ((k, ang) in listOf(-30f, -10f, 10f).withIndex()) {
        rotate(ang + 200f, pivot = a.p(0.38f, 0.62f)) {
            drawOval(listOf(pal.blue, pal.bamboo, pal.red)[k], topLeft = Offset(a.p(0.38f, 0.62f).x - u * 0.06f, a.p(0.38f, 0.62f).y), size = Size(u * 0.12f, u * 0.42f))
        }
    }
    // body
    drawOval(pal.bamboo, topLeft = Offset(c.x - u * 0.28f, c.y - u * 0.2f), size = Size(u * 0.56f, u * 0.4f))
    // wing
    drawOval(pal.bambooDark, topLeft = Offset(c.x - u * 0.15f, c.y - u * 0.1f), size = Size(u * 0.3f, u * 0.18f))
    // head
    val head = Offset(c.x + u * 0.26f, c.y - u * 0.24f)
    drawCircle(pal.bamboo, u * 0.15f, head)
    drawCircle(pal.faceTop, u * 0.045f, Offset(head.x + u * 0.04f, head.y - u * 0.02f))
    drawCircle(pal.ink, u * 0.025f, Offset(head.x + u * 0.045f, head.y - u * 0.02f))
    // beak
    val beak = Path().apply {
        moveTo(head.x + u * 0.13f, head.y - u * 0.03f)
        lineTo(head.x + u * 0.26f, head.y + u * 0.02f)
        lineTo(head.x + u * 0.12f, head.y + u * 0.06f)
        close()
    }
    drawPath(beak, pal.orange)
    // crest
    drawCircle(pal.red, u * 0.05f, Offset(head.x - u * 0.02f, head.y - u * 0.15f))
}

// ------------------------------------------------------------------ characters

/** Brush strokes of the Chinese numerals 1–9 in a unit box. */
private val NUMERALS: Array<List<List<Offset>>> = arrayOf(
    listOf(listOf(Offset(.08f, .52f), Offset(.5f, .48f), Offset(.92f, .5f))),
    listOf(listOf(Offset(.22f, .3f), Offset(.78f, .28f)), listOf(Offset(.08f, .74f), Offset(.92f, .72f))),
    listOf(listOf(Offset(.2f, .16f), Offset(.8f, .14f)), listOf(Offset(.28f, .48f), Offset(.72f, .47f)), listOf(Offset(.08f, .84f), Offset(.92f, .82f))),
    listOf(
        listOf(Offset(.1f, .16f), Offset(.1f, .88f)),
        listOf(Offset(.1f, .16f), Offset(.9f, .16f)),
        listOf(Offset(.9f, .16f), Offset(.9f, .88f)),
        listOf(Offset(.1f, .86f), Offset(.9f, .86f)),
        listOf(Offset(.4f, .18f), Offset(.3f, .62f)),
        listOf(Offset(.6f, .18f), Offset(.6f, .6f)),
        listOf(Offset(.6f, .6f), Offset(.84f, .62f)),
    ),
    listOf(
        listOf(Offset(.16f, .12f), Offset(.84f, .1f)),
        listOf(Offset(.46f, .12f), Offset(.38f, .86f)),
        listOf(Offset(.24f, .46f), Offset(.74f, .44f), Offset(.7f, .86f)),
        listOf(Offset(.06f, .87f), Offset(.94f, .86f)),
    ),
    listOf(
        listOf(Offset(.46f, .04f), Offset(.56f, .2f)),
        listOf(Offset(.08f, .34f), Offset(.92f, .32f)),
        listOf(Offset(.4f, .5f), Offset(.3f, .72f), Offset(.14f, .9f)),
        listOf(Offset(.6f, .52f), Offset(.74f, .72f), Offset(.88f, .88f)),
    ),
    listOf(
        listOf(Offset(.06f, .5f), Offset(.94f, .36f)),
        listOf(Offset(.38f, .08f), Offset(.38f, .78f), Offset(.48f, .88f), Offset(.9f, .86f)),
    ),
    listOf(
        listOf(Offset(.42f, .14f), Offset(.36f, .5f), Offset(.1f, .88f)),
        listOf(Offset(.58f, .14f), Offset(.66f, .5f), Offset(.92f, .86f)),
    ),
    listOf(
        listOf(Offset(.48f, .06f), Offset(.44f, .5f), Offset(.12f, .9f)),
        listOf(Offset(.14f, .36f), Offset(.68f, .34f), Offset(.68f, .8f), Offset(.92f, .84f), Offset(.94f, .7f)),
    ),
)

/** Simplified "wan" (ten thousand) – the suit sign of the character tiles. */
private val WAN: List<List<Offset>> = listOf(
    listOf(Offset(.08f, .14f), Offset(.92f, .12f)),
    listOf(Offset(.4f, .14f), Offset(.36f, .55f), Offset(.12f, .92f)),
    listOf(Offset(.38f, .42f), Offset(.8f, .4f), Offset(.74f, .86f), Offset(.6f, .8f)),
)

private fun DrawScope.strokes(list: List<List<Offset>>, l: Float, t: Float, w: Float, h: Float, color: Color, width: Float) {
    val style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
    for (pts in list) {
        val p = pts.map { Offset(l + it.x * w, t + it.y * h) }
        val path = Path()
        path.moveTo(p[0].x, p[0].y)
        if (p.size == 2) {
            path.lineTo(p[1].x, p[1].y)
        } else {
            for (i in 1 until p.size - 1) {
                val mid = Offset((p[i].x + p[i + 1].x) / 2, (p[i].y + p[i + 1].y) / 2)
                if (i == p.size - 2) path.quadraticTo(p[i].x, p[i].y, p[i + 1].x, p[i + 1].y)
                else path.quadraticTo(p[i].x, p[i].y, mid.x, mid.y)
            }
        }
        drawPath(path, color, style = style)
    }
}

private fun DrawScope.drawCharacter(n: Int, a: Area, pal: TilePalette) {
    val sw = a.w * 0.1f * pal.strokeScale
    strokes(NUMERALS[n - 1], a.l + a.w * 0.12f, a.t + a.h * 0.02f, a.w * 0.76f, a.h * 0.4f, pal.ink, sw)
    strokes(WAN, a.l + a.w * 0.14f, a.t + a.h * 0.52f, a.w * 0.72f, a.h * 0.44f, pal.red, sw)
}

// ------------------------------------------------------------------ winds

private fun DrawScope.drawWind(dir: Int, a: Area, pal: TilePalette, letter: String) {
    val c = a.p(0.5f, 0.36f)
    val rad = a.w * 0.36f
    drawCircle(pal.ink.copy(alpha = 0.35f), rad, c, style = Stroke(a.w * 0.025f * pal.strokeScale))
    for (k in 0 until 8) {
        val ang = k * PI / 4
        val inner = if (k % 2 == 0) 0.78f else 0.86f
        drawLine(
            pal.ink.copy(alpha = 0.45f),
            Offset(c.x + cos(ang).toFloat() * rad * inner, c.y + sin(ang).toFloat() * rad * inner),
            Offset(c.x + cos(ang).toFloat() * rad, c.y + sin(ang).toFloat() * rad),
            strokeWidth = a.w * 0.025f,
        )
    }
    // east = 0°, south = 90°, west = 180°, north = 270° (screen coordinates)
    val angle = when (dir) { 1 -> 0f; 2 -> 90f; 3 -> 180f; else -> 270f }
    rotate(angle, pivot = c) {
        val arrow = Path().apply {
            moveTo(c.x + rad * 0.9f, c.y)
            lineTo(c.x - rad * 0.25f, c.y - rad * 0.32f)
            lineTo(c.x - rad * 0.05f, c.y)
            lineTo(c.x - rad * 0.25f, c.y + rad * 0.32f)
            close()
        }
        drawPath(arrow, pal.red)
        drawCircle(pal.ink, rad * 0.1f, c)
    }
    drawLabel(letter, a.p(0.5f, 0.84f), a.w * 0.5f, pal.ink, bold = true)
}

// ------------------------------------------------------------------ dragons

private fun DrawScope.drawDragon(kind: Int, a: Area, pal: TilePalette) {
    when (kind) {
        1 -> { // red "middle"
            val sw = a.w * 0.11f * pal.strokeScale
            val style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawRoundRect(pal.red, topLeft = a.p(0.12f, 0.3f), size = Size(a.w * 0.76f, a.h * 0.32f), cornerRadius = CornerRadius(sw * 0.3f), style = style)
            drawLine(pal.red, a.p(0.5f, 0.08f), a.p(0.5f, 0.94f), strokeWidth = sw, cap = StrokeCap.Round)
        }
        2 -> { // green: three jade leaves
            val base = a.p(0.5f, 0.86f)
            for (ang in listOf(-38f, 0f, 38f)) {
                rotate(ang, pivot = base) {
                    val leaf = Path().apply {
                        moveTo(base.x, base.y)
                        quadraticTo(base.x - a.w * 0.3f, base.y - a.h * 0.42f, base.x, base.y - a.h * 0.74f)
                        quadraticTo(base.x + a.w * 0.3f, base.y - a.h * 0.42f, base.x, base.y)
                        close()
                    }
                    drawPath(leaf, pal.green)
                    drawLine(pal.faceTop.copy(alpha = 0.7f), base, Offset(base.x, base.y - a.h * 0.62f), strokeWidth = a.w * 0.025f)
                }
            }
            drawCircle(pal.red, a.w * 0.07f, base)
        }
        else -> { // white: blue double frame
            val sw = a.w * 0.07f * pal.strokeScale
            drawRect(pal.blue, topLeft = a.p(0.06f, 0.06f), size = Size(a.w * 0.88f, a.h * 0.88f), style = Stroke(sw))
            drawRect(pal.blue, topLeft = a.p(0.22f, 0.18f), size = Size(a.w * 0.56f, a.h * 0.64f), style = Stroke(sw * 0.5f))
            for ((x, y) in listOf(0.06f to 0.06f, 0.94f to 0.06f, 0.06f to 0.94f, 0.94f to 0.94f)) {
                val p = a.p(x, y)
                drawRect(pal.blue, topLeft = Offset(p.x - sw, p.y - sw), size = Size(sw * 2, sw * 2))
            }
        }
    }
}

// ------------------------------------------------------------------ flowers & seasons

private fun DrawScope.blossom(c: Offset, r: Float, petals: Int, color: Color, center: Color) {
    for (k in 0 until petals) {
        val ang = k * 2 * PI / petals - PI / 2
        drawCircle(color, r * 0.48f, Offset(c.x + cos(ang).toFloat() * r * 0.55f, c.y + sin(ang).toFloat() * r * 0.55f))
    }
    drawCircle(center, r * 0.3f, c)
}

private fun DrawScope.leafPath(base: Offset, len: Float, width: Float): Path = Path().apply {
    moveTo(base.x, base.y)
    quadraticTo(base.x - width, base.y - len * 0.5f, base.x, base.y - len)
    quadraticTo(base.x + width, base.y - len * 0.5f, base.x, base.y)
    close()
}

private fun DrawScope.drawFlower(kind: Int, a: Area, pal: TilePalette) {
    val u = a.w
    when (kind) {
        1 -> { // plum
            drawLine(Color(0xFF6D4C33), a.p(0.1f, 0.95f), a.p(0.6f, 0.45f), strokeWidth = u * 0.06f, cap = StrokeCap.Round)
            drawLine(Color(0xFF6D4C33), a.p(0.42f, 0.63f), a.p(0.8f, 0.72f), strokeWidth = u * 0.04f, cap = StrokeCap.Round)
            blossom(a.p(0.55f, 0.38f), u * 0.3f, 5, pal.pink, pal.yellow)
            blossom(a.p(0.8f, 0.72f), u * 0.17f, 5, pal.pink, pal.yellow)
            blossom(a.p(0.22f, 0.25f), u * 0.13f, 5, pal.pink.copy(alpha = 0.8f), pal.yellow)
        }
        2 -> { // orchid
            for (ang in listOf(-25f, 25f)) rotate(ang, pivot = a.p(0.5f, 0.95f)) { drawPath(leafPath(a.p(0.5f, 0.95f), a.h * 0.55f, u * 0.12f), pal.green) }
            val c = a.p(0.5f, 0.38f)
            for (ang in listOf(-60f, 60f, 180f)) rotate(ang, pivot = c) { drawPath(leafPath(c, a.h * 0.32f, u * 0.16f), pal.purple) }
            drawCircle(pal.yellow, u * 0.07f, c)
        }
        3 -> { // chrysanthemum
            val c = a.p(0.5f, 0.42f)
            for (k in 0 until 16) {
                rotate(k * 22.5f, pivot = c) {
                    drawLine(pal.yellow, c, Offset(c.x, c.y - u * 0.36f), strokeWidth = u * 0.07f, cap = StrokeCap.Round)
                }
            }
            drawCircle(pal.orange, u * 0.12f, c)
            drawLine(pal.green, a.p(0.5f, 0.66f), a.p(0.5f, 0.98f), strokeWidth = u * 0.05f)
            rotate(40f, pivot = a.p(0.5f, 0.9f)) { drawPath(leafPath(a.p(0.5f, 0.9f), a.h * 0.22f, u * 0.08f), pal.green) }
        }
        else -> { // bamboo flower
            drawLine(pal.bambooDark, a.p(0.5f, 0.98f), a.p(0.5f, 0.1f), strokeWidth = u * 0.07f, cap = StrokeCap.Round)
            for ((y, ang) in listOf(0.3f to -55f, 0.45f to 55f, 0.62f to -50f, 0.78f to 50f)) {
                rotate(ang, pivot = a.p(0.5f, y)) { drawPath(leafPath(a.p(0.5f, y), a.h * 0.32f, u * 0.08f), pal.bamboo) }
            }
        }
    }
}

private fun DrawScope.drawSeason(kind: Int, a: Area, pal: TilePalette) {
    val u = a.w
    when (kind) {
        1 -> { // spring: sprout with bud
            drawLine(pal.green, a.p(0.5f, 0.95f), a.p(0.5f, 0.3f), strokeWidth = u * 0.06f, cap = StrokeCap.Round)
            rotate(-50f, pivot = a.p(0.5f, 0.62f)) { drawPath(leafPath(a.p(0.5f, 0.62f), a.h * 0.34f, u * 0.13f), pal.green) }
            rotate(50f, pivot = a.p(0.5f, 0.72f)) { drawPath(leafPath(a.p(0.5f, 0.72f), a.h * 0.3f, u * 0.12f), pal.bamboo) }
            blossom(a.p(0.5f, 0.24f), u * 0.2f, 5, pal.pink, pal.yellow)
        }
        2 -> { // summer: sun
            val c = a.p(0.5f, 0.48f)
            for (k in 0 until 12) rotate(k * 30f, pivot = c) {
                drawLine(pal.orange, Offset(c.x, c.y - u * 0.27f), Offset(c.x, c.y - u * 0.42f), strokeWidth = u * 0.06f, cap = StrokeCap.Round)
            }
            drawCircle(pal.orange, u * 0.22f, c)
            drawCircle(pal.yellow, u * 0.15f, c)
        }
        3 -> { // autumn: maple leaf
            val c = a.p(0.5f, 0.45f)
            val path = Path()
            val points = 10
            for (k in 0..points) {
                val ang = -PI / 2 + k * 2 * PI / points
                val rr = if (k % 2 == 0) u * 0.42f else u * 0.2f
                val x = c.x + cos(ang).toFloat() * rr
                val y = c.y + sin(ang).toFloat() * rr
                if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, pal.orange)
            drawPath(path, pal.red, style = Stroke(u * 0.03f, join = StrokeJoin.Round))
            drawLine(pal.red, c, a.p(0.5f, 0.98f), strokeWidth = u * 0.05f, cap = StrokeCap.Round)
        }
        else -> { // winter: snowflake
            val c = a.p(0.5f, 0.5f)
            val len = u * 0.42f
            for (k in 0 until 6) rotate(k * 60f, pivot = c) {
                drawLine(pal.blue, c, Offset(c.x, c.y - len), strokeWidth = u * 0.05f, cap = StrokeCap.Round)
                for (f in listOf(0.45f, 0.72f)) {
                    val p = Offset(c.x, c.y - len * f)
                    drawLine(pal.blue, p, Offset(p.x - len * 0.2f, p.y - len * 0.18f), strokeWidth = u * 0.035f, cap = StrokeCap.Round)
                    drawLine(pal.blue, p, Offset(p.x + len * 0.2f, p.y - len * 0.18f), strokeWidth = u * 0.035f, cap = StrokeCap.Round)
                }
            }
            drawCircle(pal.blue, u * 0.06f, c)
        }
    }
}
