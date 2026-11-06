package com.mahjongzen.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import com.mahjongzen.game.model.Layout
import com.mahjongzen.game.model.TileFace

/** A single decorative tile (used on the start screen). */
@Composable
fun TilePreview(face: TileFace, palette: TilePalette, letters: List<String>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val d = size.width * 0.1f
        val w = size.width - d * 1.8f
        val h = size.height - d * 1.8f
        val cr = CornerRadius(w * 0.13f)
        drawRoundRect(Color.Black.copy(alpha = 0.3f), Offset(d * 1.6f, d * 1.7f), Size(w, h), cr)
        palette.back?.let { drawRoundRect(it, Offset(d, d), Size(w, h), cr) }
        drawRoundRect(palette.sideDark, Offset(d * 0.7f, d * 0.75f), Size(w, h), cr)
        drawRoundRect(palette.side, Offset(d * 0.38f, d * 0.42f), Size(w, h), cr)
        drawFaceIn(face, palette, letters, w, h)
    }
}

/** Draws the face scaled into the top-left w × h area (drawTileFace always fills the draw size). */
private fun DrawScope.drawFaceIn(face: TileFace, palette: TilePalette, letters: List<String>, w: Float, h: Float) {
    scale(w / size.width, h / size.height, pivot = Offset.Zero) {
        drawTileFace(face, palette, letters)
    }
}

/** Miniature silhouette of a layout for the level selection. */
@Composable
fun LayoutPreview(layout: Layout, tileColor: Color, edgeColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val extra = 0.1f * (layout.layers - 1) + 0.18f
        val tw = minOf(size.width / (layout.width / 2f + extra), size.height / (layout.height * 0.66f + extra))
        val th = tw * 1.32f
        val d = tw * 0.1f
        val bw = layout.width * tw / 2f + extra * tw
        val bh = layout.height * th / 2f + extra * tw
        val ox = (size.width - bw) / 2f + (layout.layers - 1) * d
        val oy = (size.height - bh) / 2f + (layout.layers - 1) * d
        val cr = CornerRadius(tw * 0.15f)
        for (s in layout.slots) {
            val l = ox + s.x * tw / 2f - s.z * d
            val t = oy + s.y * th / 2f - s.z * d
            drawRoundRect(edgeColor, Offset(l + d * 0.6f, t + d * 0.6f), Size(tw * 0.96f, th * 0.96f), cr)
            val shade = 1f - 0.07f * (layout.layers - 1 - s.z)
            drawRoundRect(
                tileColor.copy(red = tileColor.red * shade, green = tileColor.green * shade, blue = tileColor.blue * shade),
                Offset(l, t), Size(tw * 0.96f, th * 0.96f), cr,
            )
        }
    }
}
