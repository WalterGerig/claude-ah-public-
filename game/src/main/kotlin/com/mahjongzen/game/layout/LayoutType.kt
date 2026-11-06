package com.mahjongzen.game.layout

import com.mahjongzen.game.model.Difficulty
import com.mahjongzen.game.model.Layout
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/** All available board layouts. Each layout scales with the difficulty. */
enum class LayoutType {
    TURTLE, PYRAMID, FORTRESS, DRAGON, BRIDGE, SPIDER, PROCEDURAL;

    /** Builds the layout. [seed] is only used by [PROCEDURAL]. */
    fun build(difficulty: Difficulty, seed: Long = 0L): Layout = when (this) {
        TURTLE -> turtle(difficulty)
        PYRAMID -> pyramid(difficulty)
        FORTRESS -> fortress(difficulty)
        DRAGON -> dragon(difficulty)
        BRIDGE -> bridge(difficulty)
        SPIDER -> spider(difficulty)
        PROCEDURAL -> ProceduralLayout.generate(difficulty, seed)
    }
}

/** The classic "turtle" layout – 144 tiles on Expert. */
private fun turtle(d: Difficulty) = layout {
    when (d) {
        Difficulty.EASY -> {
            row(0, 0, 6, 8); row(0, 2, 4, 10); row(0, 4, 2, 12)
            row(0, 6, 2, 12); row(0, 8, 4, 10); row(0, 10, 6, 8)
            rect(1, 10, 2, 4, 4)
            rect(2, 12, 4, 2, 2)
        }
        else -> {
            row(0, 0, 2, 12); row(0, 2, 6, 8); row(0, 4, 4, 10); row(0, 6, 2, 12)
            row(0, 8, 2, 12); row(0, 10, 4, 10); row(0, 12, 6, 8); row(0, 14, 2, 12)
            rect(1, 8, 2, 6, 6)
            rect(2, 10, 4, 4, 4)
            if (d == Difficulty.HARD || d == Difficulty.EXPERT) {
                tile(0, 7, 0); tile(26, 7, 0)
                rect(3, 12, 6, 2, 2)
            }
            if (d == Difficulty.EXPERT) {
                tile(28, 7, 0)
                tile(13, 7, 4)
            }
        }
    }
}

/** A stepped pyramid; each layer is shifted by half a tile. */
private fun pyramid(d: Difficulty) = layout {
    val (w, h, layers) = when (d) {
        Difficulty.EASY -> Triple(7, 5, 3)
        Difficulty.MEDIUM -> Triple(7, 6, 4)
        Difficulty.HARD -> Triple(8, 6, 5)
        Difficulty.EXPERT -> Triple(8, 6, 6)
    }
    // Note: the top layer must have at least two tiles – a single peak tile on a half-offset
    // pyramid would be the only free tile at the start, making the board unsolvable.
    for (k in 0 until layers) rect(k, k, k, w - k, h - k)
}

/** Walls, corner towers and a central keep. */
private fun fortress(d: Difficulty) = layout {
    if (d == Difficulty.EASY) {
        rect(0, 0, 0, 8, 6)
        ring(1, 0, 0, 8, 6)
        return@layout
    }
    rect(0, 0, 0, 10, 7)
    ring(1, 0, 0, 10, 7)
    // corner towers
    for ((tx, ty) in listOf(0 to 0, 16 to 0, 0 to 10, 16 to 10)) {
        // 2x2 tower on each corner (filled on layer 1 so it is fully supported)
        rect(1, tx, ty, 2, 2)
        rect(2, tx, ty, 2, 2)
        if (d != Difficulty.MEDIUM) tile(tx + 1, ty + 1, 3)
    }
    if (d == Difficulty.HARD || d == Difficulty.EXPERT) {
        rect(1, 6, 4, 4, 3) // keep
    }
    if (d == Difficulty.EXPERT) {
        rect(2, 7, 5, 3, 2)
        rect(3, 8, 6, 2, 1)
    }
}

/** A winding dragon with spine plates, head and tail. */
private fun dragon(d: Difficulty) = layout {
    val length = when (d) { Difficulty.EASY -> 14; Difficulty.MEDIUM -> 16; Difficulty.HARD -> 18; Difficulty.EXPERT -> 20 }
    val thickness = if (d == Difficulty.EASY) 2 else 3
    val amp = 2.0
    val tail = 3
    for (c in 0 until length) {
        val r = 3 + (amp * sin(2 * PI * c / 12.0)).roundToInt()
        val x = 2 * c
        val y = 2 * r
        val t = if (c < tail) 1 else thickness
        val yy = if (c < tail) y + 2 else y
        for (k in 0 until t) tile(x, yy + 2 * k, 0)
        if (c >= tail) {
            for (k in 0 until t - 1) tile(x, y + 1 + 2 * k, 1)
            if (d != Difficulty.EASY && c % 2 == 0) tile(x, y + 2, 2)
        }
    }
    // Head: a block at the end.
    val hc = length
    val hr = 3 + (amp * sin(2 * PI * hc / 12.0)).roundToInt()
    val hx = 2 * hc
    val hy = 2 * hr - 2
    rect(0, hx, hy, 3, thickness + 2)
    rect(1, hx + 1, hy + 1, 2, thickness + 1)
    if (d != Difficulty.EASY) rect(2, hx + 2, hy + 2, 1, thickness)
    if (d == Difficulty.EXPERT) tile(hx + 2, hy + 3, 3)
}

/** Two towers connected by a long deck. */
private fun bridge(d: Difficulty) = layout {
    val w = when (d) { Difficulty.EASY -> 10; Difficulty.MEDIUM -> 12; Difficulty.HARD, Difficulty.EXPERT -> 14 }
    val towerRows = 6
    val right = 2 * (w - 3)
    // towers
    rect(0, 0, 0, 3, towerRows)
    rect(0, right, 0, 3, towerRows)
    // deck between towers
    rect(0, 6, 4, w - 6, 2)
    // layer 1: deck over the full width, tower tops
    rect(1, 6, 4, w - 6, 2)
    rect(1, 0, 0, 3, towerRows)
    rect(1, right, 0, 3, towerRows)
    if (d == Difficulty.EASY) return@layout
    // layer 2: tower spires and handrail
    rect(2, 1, 2, 2, 4)
    rect(2, right + 1, 2, 2, 4)
    row(2, 5, 6, w - 6)
    if (d == Difficulty.MEDIUM) return@layout
    rect(3, 2, 3, 1, 3)
    rect(3, right + 2, 3, 1, 3)
    if (d == Difficulty.EXPERT) {
        // arch below the deck and tower crowns
        row(0, 8, 8, w - 8)
        tile(2, 4, 4); tile(right + 2, 4, 4)
    }
}

/** A spider: round body, head and eight legs. */
private fun spider(d: Difficulty) = layout {
    val legLen = when (d) { Difficulty.EASY -> 3; Difficulty.MEDIUM -> 4; Difficulty.HARD -> 5; Difficulty.EXPERT -> 6 }
    // body (centred around x = 10..)
    val bx = 2 * legLen + 2
    val by = 6
    rect(0, bx, by, 4, 5)
    rect(1, bx + 2, by + 2, 2, 3)
    if (d != Difficulty.EASY) rect(2, bx + 3, by + 3, 1, 2)
    if (d == Difficulty.HARD || d == Difficulty.EXPERT) tile(bx + 3, by + 4, 3)
    // abdomen
    rect(0, bx + 1, by + 10, 3, 3)
    rect(1, bx + 2, by + 11, 2, 2)
    if (d != Difficulty.EASY) tile(bx + 3, by + 12, 2)
    // head
    rect(0, bx + 2, by - 4, 2, 2)
    if (d != Difficulty.EASY) tile(bx + 3, by - 3, 1)
    // legs: four per side, start rows within the body
    val starts = listOf(by, by + 2, by + 6, by + 8)
    val slopes = listOf(-2, -1, 1, 2)
    for (side in listOf(-1, 1)) {
        for (i in 0 until 4) {
            for (k in 0 until legLen) {
                val x = if (side == 1) bx + 8 + 2 * k else bx - 2 - 2 * k
                val y = starts[i] + slopes[i] * k
                tile(x, y, 0)
                if (k == 0 && d != Difficulty.EASY) tile(x, y, 1)
            }
        }
    }
}
