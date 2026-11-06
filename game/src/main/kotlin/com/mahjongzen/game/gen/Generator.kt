package com.mahjongzen.game.gen

import com.mahjongzen.game.model.Difficulty
import com.mahjongzen.game.model.Geometry
import com.mahjongzen.game.model.PairOrdering
import com.mahjongzen.game.model.TileFace
import com.mahjongzen.game.rules.Rules
import kotlin.random.Random

/** Result of a generation: the face per slot (-1 = empty) and a guaranteed solution. */
class GeneratedBoard(
    val faces: IntArray,
    /** Pairs in removal order – replaying them is a valid solution. */
    val solution: List<Pair<Int, Int>>,
)

/**
 * Reverse ("backwards") generator.
 *
 * Starting from an empty board we repeatedly pick two slots that would be *free* if we
 * placed a tile there, and put a matching pair on them. Because every placed pair is free
 * at the moment it is placed, playing the placements backwards is always a legal game –
 * so every generated board is solvable by construction.
 */
object Generator {

    private const val MAX_ATTEMPTS = 300

    /** Generates a full board for [geo] using a fresh set of faces. */
    fun generate(geo: Geometry, difficulty: Difficulty, seed: Long): GeneratedBoard {
        val rng = Random(seed)
        val target = BooleanArray(geo.size) { true }
        val candidates = mutableListOf<GeneratedBoard>()
        var attempts = 0
        while (candidates.size < difficulty.candidates && attempts < MAX_ATTEMPTS) {
            attempts++
            val pairs = orderPairs(buildPairs(geo.size / 2, difficulty.preferSinglePairs, rng), difficulty.pairOrdering, rng)
            fill(geo, target, pairs, rng)?.let { candidates += it }
        }
        check(candidates.isNotEmpty()) { "Could not generate a solvable board" }
        // Choose the candidate whose number of initially free pairs fits the difficulty.
        val scored = candidates.map { it to Rules.countAvailablePairs(geo, it.faces) }
        val pick = if (difficulty.maximizeFreePairs) {
            scored.maxBy { it.second }
        } else {
            scored.filter { it.second > 0 }.minByOrNull { it.second } ?: scored.first()
        }
        return pick.first
    }

    /**
     * Re-deals the faces still on the board onto the occupied slots so that the position
     * stays solvable. Returns null if no solvable arrangement exists / was found.
     */
    fun shuffle(geo: Geometry, faces: IntArray, seed: Long): GeneratedBoard? {
        val rng = Random(seed)
        val target = BooleanArray(geo.size) { faces[it] >= 0 }
        val byGroup = HashMap<Int, MutableList<Int>>()
        for (f in faces) if (f >= 0) byGroup.getOrPut(TileFace.of(f).matchGroup) { mutableListOf() } += f
        repeat(MAX_ATTEMPTS) {
            val pairs = ArrayList<Pair<Int, Int>>()
            for (list in byGroup.values) {
                val l = list.shuffled(rng)
                for (k in 0 until l.size / 2) pairs += l[2 * k] to l[2 * k + 1]
            }
            pairs.shuffle(rng)
            fill(geo, target, pairs, rng)?.let { return it }
        }
        return null
    }

    /** Builds [count] pairs of face ordinals. */
    internal fun buildPairs(count: Int, preferSingle: Boolean, rng: Random): List<Pair<Int, Int>> {
        require(count <= TileFace.GROUP_COUNT * 2) { "At most 144 tiles supported" }
        val groups = (0 until TileFace.GROUP_COUNT).shuffled(rng)
        // number of groups that contribute two pairs (all four tiles)
        val doubleGroups = if (preferSingle) maxOf(0, count - TileFace.GROUP_COUNT) else count / 2
        val usedGroups = if (preferSingle) minOf(count, TileFace.GROUP_COUNT) else (count + 1) / 2
        val result = ArrayList<Pair<Int, Int>>(count)
        for (gi in 0 until usedGroups) {
            val g = groups[gi]
            val faces = TileFace.facesOfGroup(g).map { it.ordinal }
            val pairsHere = if (gi < doubleGroups) 2 else 1
            if (faces.size == 1) {
                repeat(pairsHere) { result += faces[0] to faces[0] }
            } else {
                val f = faces.shuffled(rng)
                result += f[0] to f[1]
                if (pairsHere == 2) result += f[2] to f[3]
            }
        }
        check(result.size == count)
        return result
    }

    /** Orders pairs for placement (first placed = last removed). */
    internal fun orderPairs(pairs: List<Pair<Int, Int>>, ordering: PairOrdering, rng: Random): List<Pair<Int, Int>> {
        val groupOf = { p: Pair<Int, Int> -> TileFace.of(p.first).matchGroup }
        return when (ordering) {
            PairOrdering.RANDOM -> pairs.shuffled(rng)
            PairOrdering.GROUPED -> pairs.groupBy(groupOf).values.shuffled(rng).flatten()
            PairOrdering.MIXED -> {
                val chunks = pairs.groupBy(groupOf).values.flatMap { chunk ->
                    if (rng.nextBoolean()) listOf(chunk) else chunk.map { listOf(it) }
                }
                chunks.shuffled(rng).flatten()
            }
            PairOrdering.SPREAD -> {
                // Try a few random orders and keep the one with the largest minimal distance
                // between pairs of the same group.
                var best = pairs.shuffled(rng)
                var bestScore = minGroupDistance(best, groupOf)
                repeat(12) {
                    val cand = pairs.shuffled(rng)
                    val s = minGroupDistance(cand, groupOf)
                    if (s > bestScore) { best = cand; bestScore = s }
                }
                best
            }
        }
    }

    private fun minGroupDistance(list: List<Pair<Int, Int>>, groupOf: (Pair<Int, Int>) -> Int): Int {
        val last = HashMap<Int, Int>()
        var min = Int.MAX_VALUE
        list.forEachIndexed { i, p ->
            val g = groupOf(p)
            last[g]?.let { min = minOf(min, i - it) }
            last[g] = i
        }
        return min
    }

    /**
     * One reverse-fill attempt over all slots marked in [target]. Returns null on a dead end.
     */
    internal fun fill(geo: Geometry, target: BooleanArray, pairs: List<Pair<Int, Int>>, rng: Random): GeneratedBoard? {
        val n = geo.size
        val needed = target.count { it }
        if (needed != pairs.size * 2) return null
        // During generation filled[] holds 0 for "filled", -1 for "empty"; real faces are set at the end.
        val filled = IntArray(n) { -1 }
        val filledBits = LongArray((n + 63) / 64)
        val placement = IntArray(n) { -1 }
        val order = ArrayList<Pair<Int, Int>>(pairs.size)

        val comp = rowComponents(geo, target)
        val compMembers = (0 until n).filter { target[it] }.groupBy { comp[it] }
        val emptyInComp = IntArray(n)
        for (i in 0 until n) if (target[i]) emptyInComp[comp[i]]++
        val dependents = dependents(geo, target)
        val leftClosure = closures(geo, target, geo.left)
        val rightClosure = closures(geo, target, geo.right)

        fun isFilled(i: Int) = filled[i] >= 0
        fun setFilled(i: Int, value: Boolean) {
            filled[i] = if (value) 0 else -1
            val w = i / 64
            val bit = 1L shl (i % 64)
            filledBits[w] = if (value) filledBits[w] or bit else filledBits[w] and bit.inv()
            emptyInComp[comp[i]] -= if (value) 1 else -1
        }
        fun intersects(bits: LongArray): Boolean {
            for (k in bits.indices) if (bits[k] and filledBits[k] != 0L) return true
            return false
        }

        /**
         * An empty slot u is lost forever if there are filled tiles somewhere to its left AND
         * to its right (following neighbour chains): to be placed later, one complete side of u
         * would have to be placed after u, which is impossible once a tile there is filled.
         */
        fun createsDeadSlot(i: Int): Boolean {
            for (u in compMembers[comp[i]].orEmpty()) {
                if (isFilled(u)) continue
                if (intersects(leftClosure[u]) && intersects(rightClosure[u])) return true
            }
            return false
        }

        fun canPlace(i: Int): Boolean {
            if (!target[i] || isFilled(i)) return false
            for (j in geo.below[i]) if (target[j] && !isFilled(j)) return false
            for (j in geo.above[i]) if (isFilled(j)) return false
            val l = geo.left[i].any { isFilled(it) }
            val r = geo.right[i].any { isFilled(it) }
            return !(l && r)
        }

        // Rows with many open slots and slots carrying stacks need many separate steps,
        // so they are served first; otherwise the generation may end with a single row left.
        fun prioritised(list: List<Int>): List<Int> =
            list.map { it to emptyInComp[comp[it]] + dependents[it] * 0.7 + rng.nextDouble() * 4.0 }
                .sortedByDescending { it.second }
                .map { it.first }

        for (pair in pairs) {
            val cands = prioritised((0 until n).filter { canPlace(it) })
            var placed = false
            for (a in cands) {
                setFilled(a, true)
                if (createsDeadSlot(a)) { setFilled(a, false); continue }
                val partners = prioritised((0 until n).filter { it != a && canPlace(it) })
                for (b in partners) {
                    setFilled(b, true)
                    // both tiles must be free once both are on the board
                    if (Rules.isFree(geo, filled, a) && Rules.isFree(geo, filled, b) && !createsDeadSlot(b)) {
                        placement[a] = pair.first
                        placement[b] = pair.second
                        order += a to b
                        placed = true
                        break
                    }
                    setFilled(b, false)
                }
                if (placed) break
                setFilled(a, false)
            }
            if (!placed) return null
        }
        return GeneratedBoard(placement, order.asReversed().toList())
    }

    /** Transitive closure of a neighbour relation as bit sets (only target slots). */
    private fun closures(geo: Geometry, target: BooleanArray, rel: Array<IntArray>): Array<LongArray> {
        val words = (geo.size + 63) / 64
        return Array(geo.size) { i ->
            val bits = LongArray(words)
            if (target[i]) {
                val stack = ArrayDeque<Int>()
                rel[i].forEach { if (target[it]) stack.add(it) }
                while (stack.isNotEmpty()) {
                    val j = stack.removeLast()
                    val w = j / 64
                    val bit = 1L shl (j % 64)
                    if (bits[w] and bit != 0L) continue
                    bits[w] = bits[w] or bit
                    rel[j].forEach { if (target[it]) stack.add(it) }
                }
            }
            bits
        }
    }

    /** Number of target slots that lie (transitively) above each slot. */
    private fun dependents(geo: Geometry, target: BooleanArray): IntArray {
        val result = IntArray(geo.size)
        for (i in 0 until geo.size) {
            if (!target[i]) continue
            val seen = HashSet<Int>()
            val stack = ArrayDeque<Int>()
            stack.addAll(geo.above[i].filter { target[it] })
            while (stack.isNotEmpty()) {
                val j = stack.removeLast()
                if (seen.add(j)) geo.above[j].forEach { if (target[it]) stack.add(it) }
            }
            result[i] = seen.size
        }
        return result
    }

    /** Union-find over left/right adjacency of the target slots. Returns a component id per slot. */
    private fun rowComponents(geo: Geometry, target: BooleanArray): IntArray {
        val parent = IntArray(geo.size) { it }
        fun find(x: Int): Int {
            var r = x
            while (parent[r] != r) r = parent[r]
            var c = x
            while (parent[c] != r) { val nx = parent[c]; parent[c] = r; c = nx }
            return r
        }
        for (i in 0 until geo.size) {
            if (!target[i]) continue
            for (j in geo.right[i]) if (target[j]) parent[find(i)] = find(j)
        }
        return IntArray(geo.size) { find(it) }
    }
}
