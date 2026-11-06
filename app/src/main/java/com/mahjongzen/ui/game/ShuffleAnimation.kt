package com.mahjongzen.ui.game

import com.mahjongzen.game.model.Layout
import kotlin.math.min
import kotlin.random.Random

/** Timing of the shuffle animation (shared by the ViewModel, which locks input meanwhile). */
object ShuffleTiming {
    const val GATHER = 400_000_000L
    const val HOLD = 150_000_000L
    const val SCATTER = 450_000_000L
    const val FADE = 260_000_000L
    private const val STEP = 15_000_000L
    /** All staggering together never takes longer than this, even with 144 tiles. */
    private const val MAX_STAGGER_TOTAL = 300_000_000L

    fun stagger(n: Int): Long = if (n <= 1) 0L else min(STEP, MAX_STAGGER_TOTAL / (n - 1))

    fun total(n: Int, reduced: Boolean): Long =
        if (reduced) FADE else GATHER + HOLD + SCATTER + 2 * stagger(n) * (n - 1).coerceAtLeast(0)
}

/**
 * All per-tile parameters of one shuffle, pre-computed once. The board computes every
 * tile's offset/rotation/scale analytically from the frame time, so 144 tiles animate in a
 * single draw pass without any recomposition per frame.
 */
class ShuffleAnimation(
    val oldFaces: List<Int>,
    val newFaces: List<Int>,
    layout: Layout,
    val start: Long,
    val reduced: Boolean,
) {
    val tiles: IntArray = newFaces.indices.filter { newFaces[it] >= 0 }.toIntArray()
    val stagger = ShuffleTiming.stagger(tiles.size)
    val gatherEnd = ShuffleTiming.GATHER + stagger * (tiles.size - 1).coerceAtLeast(0)
    val holdEnd = gatherEnd + ShuffleTiming.HOLD
    val total = ShuffleTiming.total(tiles.size, reduced)

    private val rng = Random(start)
    /** Order in which tiles leave the board (randomised). */
    val gatherRank = IntArray(newFaces.size)
    /** Order in which tiles land again – lower layers first. */
    val scatterRank = IntArray(newFaces.size)
    /** Position in the pile relative to the board centre, in tile widths / heights. */
    val pileDx = FloatArray(newFaces.size)
    val pileDy = FloatArray(newFaces.size)
    val pileRot = FloatArray(newFaces.size)
    val pileScale = FloatArray(newFaces.size)
    /** For every new slot: the old slot of the physical tile that lands there (same face). */
    val source = IntArray(newFaces.size) { it }

    val byGather: IntArray
    val byScatter: IntArray

    init {
        tiles.toList().shuffled(rng).forEachIndexed { r, i -> gatherRank[i] = r }
        val sorted = tiles.sortedWith(compareBy({ layout.slots[it].z }, { rng.nextInt() }))
        sorted.forEachIndexed { r, i -> scatterRank[i] = r }
        for (i in tiles) {
            val a = rng.nextDouble(0.0, 2 * Math.PI)
            val r = kotlin.math.sqrt(rng.nextDouble()) * 1.4
            pileDx[i] = (kotlin.math.cos(a) * r).toFloat()
            pileDy[i] = (kotlin.math.sin(a) * r * 0.7).toFloat()
            pileRot[i] = rng.nextDouble(-15.0, 15.0).toFloat()
            pileScale[i] = rng.nextDouble(0.88, 0.94).toFloat()
        }
        val unused = HashMap<Int, ArrayDeque<Int>>()
        for (i in tiles) unused.getOrPut(oldFaces[i]) { ArrayDeque() }.add(i)
        for (j in tiles) source[j] = unused[newFaces[j]]?.removeFirstOrNull() ?: j
        byGather = tiles.sortedBy { gatherRank[it] }.toIntArray()
        byScatter = sorted.toIntArray()
    }

    fun isDone(now: Long) = now - start >= total
    fun progress(now: Long) = ((now - start).toFloat() / total).coerceIn(0f, 1f)
}
