package com.mahjongzen.game.layout

import com.mahjongzen.game.model.Difficulty
import com.mahjongzen.game.model.Layout
import com.mahjongzen.game.model.Slot
import kotlin.random.Random

/**
 * Generates random, mirror-symmetric layouts.
 * The shape is built from random rectangles and ellipses on the left half and mirrored,
 * so every layer automatically contains an even number of tiles.
 */
object ProceduralLayout {

    private data class Spec(val target: Int, val layers: Int, val halfCols: Int, val rows: Int)

    private fun spec(d: Difficulty) = when (d) {
        Difficulty.EASY -> Spec(76, 2, 6, 7)
        Difficulty.MEDIUM -> Spec(104, 3, 7, 8)
        Difficulty.HARD -> Spec(124, 4, 7, 8)
        Difficulty.EXPERT -> Spec(144, 5, 8, 8)
    }

    fun generate(d: Difficulty, seed: Long): Layout {
        val rng = Random(seed xor 0x5DEECE66DL)
        val s = spec(d)
        val cols = s.halfCols * 2
        repeat(50) {
            val result = attempt(s, cols, rng)
            if (result != null) return Layout(result)
        }
        // Fallback: simple symmetric rectangle stack
        val slots = mutableListOf<Slot>()
        for (c in 0 until cols) for (r in 0 until s.rows) slots += Slot(2 * c, 2 * r, 0)
        return Layout(slots.take(s.target - s.target % 2))
    }

    private fun attempt(s: Spec, cols: Int, rng: Random): List<Slot>? {
        val half = cols / 2
        // ---- base layer mask on the left half ----
        val mask = Array(half) { BooleanArray(s.rows) }
        val baseShare = when (s.layers) { 2 -> 0.62; 3 -> 0.5; 4 -> 0.42; else -> 0.36 }
        val baseTarget = (s.target * baseShare).toInt() / 2  // tiles on the left half
        var filled = 0
        var guard = 0
        while (filled < baseTarget && guard++ < 200) {
            val w = rng.nextInt(2, half + 1)
            val h = rng.nextInt(2, s.rows + 1)
            val x0 = rng.nextInt(0, half - w + 1)
            val y0 = rng.nextInt(0, s.rows - h + 1)
            val ellipse = rng.nextBoolean()
            for (c in x0 until x0 + w) for (r in y0 until y0 + h) {
                if (ellipse) {
                    val dx = (c - x0 + 0.5) / w - 0.5
                    val dy = (r - y0 + 0.5) / h - 0.5
                    if (dx * dx + dy * dy > 0.25) continue
                }
                if (!mask[c][r] && filled < baseTarget) { mask[c][r] = true; filled++ }
            }
        }
        // ensure connection to the mirror axis so the shape is one piece
        for (r in 0 until s.rows) if (mask[half - 2].getOrElse(r) { false }) mask[half - 1][r] = true

        val slots = LinkedHashSet<Slot>()
        for (c in 0 until half) for (r in 0 until s.rows) if (mask[c][r]) {
            slots += Slot(2 * c, 2 * r, 0)
            slots += Slot(2 * (cols - 1 - c), 2 * r, 0)
        }
        if (slots.size > s.target) return null

        // ---- upper layers ----
        for (z in 1 until s.layers) {
            if (slots.size >= s.target) break
            val offsetY = if (rng.nextInt(3) == 0) 1 else 0
            val below = slots.filter { it.z == z - 1 }
            val candidates = ArrayList<Slot>()
            for (c in 0 until half) for (yy in 0 until 2 * s.rows step 2) {
                val cand = Slot(2 * c, yy + offsetY, z)
                if (fullySupported(cand, below)) candidates += cand
            }
            // prefer interior positions: shuffle, then keep with a probability
            candidates.shuffle(rng)
            // spread the remaining tiles over the remaining layers
            val quota = slots.size + (s.target - slots.size) / (s.layers - z)
            val limit = if (z == s.layers - 1) s.target else quota
            for (cand in candidates) {
                if (slots.size + 2 > limit) break
                val mirror = Slot(2 * (cols - 1) - cand.x, cand.y, z)
                if (slots.any { it.z == z && (it.overlaps(cand) || it.overlaps(mirror)) }) continue
                slots += cand; slots += mirror
            }
        }
        if (slots.size < s.target * 0.8) return null
        return slots.toList()
    }

    /** Every quarter of the tile must lie on a tile of the layer below. */
    private fun fullySupported(s: Slot, below: List<Slot>): Boolean {
        for (qx in 0..1) for (qy in 0..1) {
            val px = s.x + qx
            val py = s.y + qy
            if (below.none { px >= it.x && px < it.x + 2 && py >= it.y && py < it.y + 2 }) return false
        }
        return true
    }
}
