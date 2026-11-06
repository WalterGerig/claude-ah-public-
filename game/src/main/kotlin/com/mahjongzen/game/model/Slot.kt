package com.mahjongzen.game.model

/**
 * A position on the board. [x] and [y] are measured in *half tile* units, so a tile
 * occupies the area [x, x+2) × [y, y+2). This allows the typical half-offset placements.
 * [z] is the layer (0 = table).
 */
data class Slot(val x: Int, val y: Int, val z: Int) {
    /** True if both tiles share area when projected onto the table. */
    fun overlaps(o: Slot): Boolean = kotlin.math.abs(x - o.x) < 2 && kotlin.math.abs(y - o.y) < 2
}
