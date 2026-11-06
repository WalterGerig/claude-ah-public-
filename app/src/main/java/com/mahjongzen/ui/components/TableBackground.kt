package com.mahjongzen.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mahjongzen.data.settings.TableBackground
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** Pre-computed random details so the background never flickers between frames. */
class TableTextures {
    val noise: ImageBitmap = run {
        val n = 96
        val rng = Random(11)
        val px = IntArray(n * n) {
            val v = rng.nextInt(0, 256)
            val a = rng.nextInt(6, 24)
            (a shl 24) or (v shl 16) or (v shl 8) or v
        }
        Bitmap.createBitmap(px, n, n, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
    val noiseBrush = ShaderBrush(ImageShader(noise, TileMode.Repeated, TileMode.Repeated))
    val stars: List<Triple<Float, Float, Float>> = Random(3).let { r -> List(90) { Triple(r.nextFloat(), r.nextFloat(), r.nextFloat()) } }
    val grain: List<Triple<Float, Float, Float>> = Random(5).let { r -> List(70) { Triple(r.nextFloat(), r.nextFloat(), r.nextFloat()) } }
}

@Composable
fun TableBackgroundView(background: TableBackground, modifier: Modifier = Modifier) {
    val tex = remember { TableTextures() }
    Canvas(modifier.fillMaxSize()) { drawTable(background, tex) }
}

fun DrawScope.drawTable(bg: TableBackground, tex: TableTextures) {
    val w = size.width
    val h = size.height
    when (bg) {
        TableBackground.FELT -> {
            drawRect(Brush.radialGradient(listOf(Color(0xFF2F7D56), Color(0xFF1F5A3E), Color(0xFF173F2D)), center = center, radius = max(w, h) * 0.75f))
            drawRect(tex.noiseBrush, alpha = 0.55f)
            woodRim(min(w, h) * 0.03f)
        }
        TableBackground.DARK_WOOD -> {
            val planks = max(5, (w / 140f).toInt())
            val pw = w / planks
            for (i in 0 until planks) {
                val c = if (i % 2 == 0) Color(0xFF3A2516) else Color(0xFF42291A)
                drawRect(c, topLeft = Offset(i * pw, 0f), size = Size(pw, h))
                drawLine(Color(0x55000000), Offset(i * pw, 0f), Offset(i * pw, h), strokeWidth = 2f)
            }
            for ((x, y, l) in tex.grain) {
                val gx = x * w
                drawLine(Color(0x14FFE0B0), Offset(gx, y * h), Offset(gx + 6f, y * h + l * h * 0.4f), strokeWidth = 1.5f)
            }
            drawRect(tex.noiseBrush, alpha = 0.35f)
        }
        TableBackground.BAMBOO -> {
            drawRect(Color(0xFF55603A))
            val stalks = max(6, (w / 90f).toInt())
            val sw = w / stalks
            for (i in 0 until stalks) {
                val x = i * sw
                drawRect(
                    Brush.horizontalGradient(listOf(Color(0xFF5F6B40), Color(0xFF76844F), Color(0xFF5A653C)), startX = x, endX = x + sw),
                    topLeft = Offset(x + sw * 0.06f, 0f), size = Size(sw * 0.88f, h),
                )
                var y = (i * 37 % 100) / 100f * sw * 3
                while (y < h) {
                    drawLine(Color(0x55303A1E), Offset(x + sw * 0.06f, y), Offset(x + sw * 0.94f, y), strokeWidth = 3f)
                    y += sw * 3.2f
                }
            }
            drawRect(tex.noiseBrush, alpha = 0.3f)
        }
        TableBackground.NIGHT -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF13233A), Color(0xFF0A1220), Color(0xFF05080F))))
            drawCircle(Brush.radialGradient(listOf(Color(0x332E5A88), Color.Transparent), center = Offset(w * 0.85f, h * 0.1f), radius = min(w, h) * 0.6f), radius = min(w, h) * 0.6f, center = Offset(w * 0.85f, h * 0.1f))
            for ((x, y, s) in tex.stars) drawCircle(Color.White.copy(alpha = 0.15f + 0.35f * s), 0.6f + s * 1.6f, Offset(x * w, y * h))
        }
        TableBackground.GRADIENT -> {
            drawRect(Brush.linearGradient(listOf(Color(0xFF3F6E68), Color(0xFF2A4558), Color(0xFF243246)), start = Offset.Zero, end = Offset(w, h)))
        }
    }
    // vignette
    drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Transparent, Color(0x73000000)), center = center, radius = max(w, h) * 0.78f))
}

private fun DrawScope.woodRim(t: Float) {
    val w = size.width
    val h = size.height
    val wood = Brush.linearGradient(listOf(Color(0xFF6B4528), Color(0xFF4E301B), Color(0xFF734B2C)), start = Offset.Zero, end = Offset(w, h))
    drawRect(wood, topLeft = Offset.Zero, size = Size(w, t))
    drawRect(wood, topLeft = Offset(0f, h - t), size = Size(w, t))
    drawRect(wood, topLeft = Offset.Zero, size = Size(t, h))
    drawRect(wood, topLeft = Offset(w - t, 0f), size = Size(t, h))
    // inner edge: dark shadow + subtle highlight
    drawRect(Color(0x66000000), topLeft = Offset(t, t), size = Size(w - 2 * t, h - 2 * t), style = Stroke(t * 0.25f))
    drawRect(Color(0x22FFFFFF), topLeft = Offset(t * 0.15f, t * 0.15f), size = Size(w - 0.3f * t, h - 0.3f * t), style = Stroke(1.5f))
}
