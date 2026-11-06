package com.mahjongzen.game.layout

import com.mahjongzen.game.model.Layout
import com.mahjongzen.game.model.Slot

/**
 * Small DSL to describe layouts. Coordinates passed to [rect], [tile] … are in *half tile*
 * units, i.e. two tiles side by side are at x = 0 and x = 2.
 */
class LayoutBuilder {
    private val slots = LinkedHashSet<Slot>()

    fun tile(x: Int, y: Int, z: Int) { slots += Slot(x, y, z) }

    /** [cols] × [rows] tiles starting at (x0, y0). */
    fun rect(z: Int, x0: Int, y0: Int, cols: Int, rows: Int) {
        for (c in 0 until cols) for (r in 0 until rows) tile(x0 + 2 * c, y0 + 2 * r, z)
    }

    /** A single horizontal row. */
    fun row(z: Int, y: Int, x0: Int, count: Int) = rect(z, x0, y, count, 1)

    /** Outline of a rectangle. */
    fun ring(z: Int, x0: Int, y0: Int, cols: Int, rows: Int) {
        for (c in 0 until cols) for (r in 0 until rows) {
            if (c == 0 || r == 0 || c == cols - 1 || r == rows - 1) tile(x0 + 2 * c, y0 + 2 * r, z)
        }
    }

    fun count() = slots.size

    /**
     * Makes sure the layout has an even number of tiles (needed for pairs) by removing
     * the top-most, right-most tile that has nothing above it.
     */
    fun build(): Layout {
        if (slots.size % 2 == 1) {
            val removable = slots.filter { s -> slots.none { it.z == s.z + 1 && it.overlaps(s) } }
            slots -= removable.maxWith(compareBy<Slot>({ it.z }, { it.x }, { it.y }))
        }
        return Layout(slots)
    }
}

fun layout(block: LayoutBuilder.() -> Unit): Layout = LayoutBuilder().apply(block).build()
