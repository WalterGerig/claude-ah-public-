package com.mahjongzen.game.model

/**
 * An immutable board layout: the list of slots, sorted in drawing order
 * (layer, then x, then y) and normalised so that the minimum x / y is 0.
 */
class Layout(slots: Collection<Slot>) {
    val slots: List<Slot>
    val size: Int get() = slots.size
    val width: Int   // in half units, including the last tile
    val height: Int
    val layers: Int

    init {
        require(slots.isNotEmpty()) { "Layout must not be empty" }
        val minX = slots.minOf { it.x }
        val minY = slots.minOf { it.y }
        this.slots = slots.map { Slot(it.x - minX, it.y - minY, it.z) }
            .distinct()
            .sortedWith(compareBy<Slot>({ it.z }, { it.x }, { it.y }))
        width = this.slots.maxOf { it.x } + 2
        height = this.slots.maxOf { it.y } + 2
        layers = this.slots.maxOf { it.z } + 1
    }

    val geometry: Geometry by lazy { Geometry(this.slots) }
}

/**
 * Pre-computed neighbourhood relations used by the rules, generator and solver.
 * All arrays are indexed by slot index.
 */
class Geometry(val slots: List<Slot>) {
    val size = slots.size
    /** Slots in layer z+1 overlapping this slot (they cover it). */
    val above: Array<IntArray>
    /** Slots in layer z-1 overlapping this slot (they support it). */
    val below: Array<IntArray>
    /** Slots in the same layer directly touching on the left. */
    val left: Array<IntArray>
    /** Slots in the same layer directly touching on the right. */
    val right: Array<IntArray>

    init {
        val a = Array(size) { mutableListOf<Int>() }
        val b = Array(size) { mutableListOf<Int>() }
        val l = Array(size) { mutableListOf<Int>() }
        val r = Array(size) { mutableListOf<Int>() }
        for (i in 0 until size) {
            val s = slots[i]
            for (j in 0 until size) {
                if (i == j) continue
                val t = slots[j]
                val dy = kotlin.math.abs(s.y - t.y)
                if (t.z == s.z + 1 && s.overlaps(t)) a[i] += j
                if (t.z == s.z - 1 && s.overlaps(t)) b[i] += j
                if (t.z == s.z && dy < 2) {
                    if (t.x - s.x == -2) l[i] += j
                    if (t.x - s.x == 2) r[i] += j
                }
            }
        }
        above = Array(size) { a[it].toIntArray() }
        below = Array(size) { b[it].toIntArray() }
        left = Array(size) { l[it].toIntArray() }
        right = Array(size) { r[it].toIntArray() }
    }

    /** Same-layer overlap = invalid layout. */
    fun hasSameLayerOverlap(): Boolean {
        for (i in 0 until size) for (j in i + 1 until size) {
            if (slots[i].z == slots[j].z && slots[i].overlaps(slots[j])) return true
        }
        return false
    }
}
