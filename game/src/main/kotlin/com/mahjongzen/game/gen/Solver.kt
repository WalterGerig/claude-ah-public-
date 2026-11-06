package com.mahjongzen.game.gen

import com.mahjongzen.game.model.Geometry
import com.mahjongzen.game.model.TileFace
import com.mahjongzen.game.rules.Rules

/**
 * Independent depth-first solver with memoisation of dead positions.
 * Used by tests to verify that generated boards really are solvable.
 */
class Solver(private val geo: Geometry, private val nodeLimit: Int = 2_000_000) {

    var nodes = 0
        private set
    var limitReached = false
        private set

    private val dead = HashSet<Key>()

    /** Tiles further left / right in the same row (transitively). */
    private val leftChain: Array<IntArray> = chains(geo.left)
    private val rightChain: Array<IntArray> = chains(geo.right)

    private fun chains(rel: Array<IntArray>): Array<IntArray> = Array(geo.size) { i ->
        val seen = LinkedHashSet<Int>()
        val stack = ArrayDeque<Int>()
        rel[i].forEach { stack.add(it) }
        while (stack.isNotEmpty()) {
            val j = stack.removeLast()
            if (seen.add(j)) rel[j].forEach { stack.add(it) }
        }
        seen.toIntArray()
    }

    /** How much a tile is "in the way": tiles beneath it and the longer row side it closes. */
    private fun blockingScore(faces: IntArray, i: Int): Int {
        val under = geo.below[i].count { faces[it] >= 0 }
        val l = leftChain[i].count { faces[it] >= 0 }
        val r = rightChain[i].count { faces[it] >= 0 }
        return under * 3 + maxOf(l, r) + geo.slots[i].z * 2
    }

    private var noise: kotlin.random.Random? = null
    private var budget = 0

    /**
     * Returns a solution (removal order) or null if none was found.
     * Uses restarts with growing budgets and randomised move ordering to avoid getting stuck
     * deep in a hopeless sub-tree. Dead positions are remembered across restarts.
     */
    fun solve(startFaces: IntArray): List<Pair<Int, Int>>? {
        nodes = 0
        limitReached = false
        dead.clear()
        var restart = 0
        var restartBudget = 2_000
        while (nodes < nodeLimit) {
            noise = if (restart == 0) null else kotlin.random.Random(restart)
            budget = minOf(nodeLimit, nodes + restartBudget)
            limitReached = false
            val faces = startFaces.copyOf()
            val path = ArrayList<Pair<Int, Int>>()
            if (dfs(faces, path)) return path.toList()
            if (!limitReached) return null // search space exhausted: proven unsolvable
            restart++
            restartBudget = restartBudget * 3 / 2
        }
        limitReached = true
        return null
    }

    private fun dfs(faces: IntArray, path: MutableList<Pair<Int, Int>>): Boolean {
        if (Rules.remainingTiles(faces) == 0) return true
        if (++nodes > budget) { limitReached = true; return false }
        val key = Key.of(faces)
        if (key in dead) return false

        val free = Rules.freeTiles(geo, faces)
        val freeByGroup = HashMap<Int, MutableList<Int>>()
        for (i in free) freeByGroup.getOrPut(TileFace.of(faces[i]).matchGroup) { mutableListOf() } += i
        val remainingByGroup = IntArray(TileFace.GROUP_COUNT)
        for (f in faces) if (f >= 0) remainingByGroup[TileFace.of(f).matchGroup]++

        // Safe move: if every remaining tile of a group is free, removing a pair of it can never hurt.
        for ((g, list) in freeByGroup) {
            if (list.size >= 2 && list.size == remainingByGroup[g]) {
                return tryMove(faces, path, list[0], list[1]) ?: false
            }
        }

        // Otherwise branch. Prefer moves that free many tiles (simple heuristic).
        val moves = ArrayList<Pair<Int, Int>>()
        for (list in freeByGroup.values) for (x in list.indices) for (y in x + 1 until list.size) moves += list[x] to list[y]
        val rnd = noise
        val ordered = moves.map { (a, b) ->
            (a to b) to blockingScore(faces, a) + blockingScore(faces, b) + (rnd?.nextDouble()?.times(10.0) ?: 0.0)
        }.sortedByDescending { it.second }.map { it.first }
        moves.clear(); moves.addAll(ordered)
        for ((a, b) in moves) {
            if (tryMove(faces, path, a, b) == true) return true
            if (limitReached) return false
        }
        dead += key
        return false
    }

    private fun tryMove(faces: IntArray, path: MutableList<Pair<Int, Int>>, a: Int, b: Int): Boolean? {
        val fa = faces[a]; val fb = faces[b]
        faces[a] = -1; faces[b] = -1
        path += a to b
        val ok = dfs(faces, path)
        if (!ok) {
            path.removeAt(path.size - 1)
            faces[a] = fa; faces[b] = fb
        }
        return ok
    }

    private class Key(private val bits: LongArray) {
        override fun equals(other: Any?) = other is Key && bits.contentEquals(other.bits)
        override fun hashCode() = bits.contentHashCode()
        companion object {
            fun of(faces: IntArray): Key {
                val bits = LongArray((faces.size + 63) / 64)
                for (i in faces.indices) if (faces[i] >= 0) bits[i / 64] = bits[i / 64] or (1L shl (i % 64))
                return Key(bits)
            }
        }
    }

    companion object {
        /** Replays a solution and checks every step against the rules. */
        fun verify(geo: Geometry, startFaces: IntArray, solution: List<Pair<Int, Int>>): Boolean {
            val faces = startFaces.copyOf()
            for ((a, b) in solution) {
                if (!Rules.canRemove(geo, faces, a, b)) return false
                faces[a] = -1; faces[b] = -1
            }
            return Rules.remainingTiles(faces) == 0
        }
    }
}
