package com.mahjongzen.game.rules

import com.mahjongzen.game.model.Geometry
import com.mahjongzen.game.model.TileFace

/**
 * The core rules of Mahjong Solitaire.
 * `faces[i]` is the ordinal of the face lying on slot i or -1 if the slot is empty.
 */
object Rules {

    /** A tile is free if nothing lies on it and its left or right side is open. */
    fun isFree(geo: Geometry, faces: IntArray, i: Int): Boolean {
        if (faces[i] < 0) return false
        for (j in geo.above[i]) if (faces[j] >= 0) return false
        var leftBlocked = false
        for (j in geo.left[i]) if (faces[j] >= 0) { leftBlocked = true; break }
        if (!leftBlocked) return true
        for (j in geo.right[i]) if (faces[j] >= 0) return false
        return true
    }

    fun freeTiles(geo: Geometry, faces: IntArray): List<Int> =
        (0 until geo.size).filter { isFree(geo, faces, it) }

    fun matches(faces: IntArray, a: Int, b: Int): Boolean =
        a != b && faces[a] >= 0 && faces[b] >= 0 && TileFace.of(faces[a]).matches(TileFace.of(faces[b]))

    /** Can tiles [a] and [b] be removed right now? */
    fun canRemove(geo: Geometry, faces: IntArray, a: Int, b: Int): Boolean =
        matches(faces, a, b) && isFree(geo, faces, a) && isFree(geo, faces, b)

    /** All currently removable pairs. */
    fun availablePairs(geo: Geometry, faces: IntArray): List<Pair<Int, Int>> {
        val byGroup = HashMap<Int, MutableList<Int>>()
        for (i in freeTiles(geo, faces)) {
            byGroup.getOrPut(TileFace.of(faces[i]).matchGroup) { mutableListOf() } += i
        }
        val result = ArrayList<Pair<Int, Int>>()
        for (list in byGroup.values) {
            for (x in list.indices) for (y in x + 1 until list.size) result += list[x] to list[y]
        }
        return result
    }

    fun countAvailablePairs(geo: Geometry, faces: IntArray): Int {
        val counts = IntArray(TileFace.GROUP_COUNT)
        for (i in 0 until geo.size) if (isFree(geo, faces, i)) counts[TileFace.of(faces[i]).matchGroup]++
        return counts.sumOf { it * (it - 1) / 2 }
    }

    fun remainingTiles(faces: IntArray): Int = faces.count { it >= 0 }
}
