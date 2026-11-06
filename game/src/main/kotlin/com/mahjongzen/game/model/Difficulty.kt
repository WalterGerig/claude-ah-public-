package com.mahjongzen.game.model

/** How the face pairs are ordered during reverse generation. */
enum class PairOrdering {
    /** Both pairs of a face are placed directly after each other -> they become free together. */
    GROUPED,
    /** Half grouped, half random. */
    MIXED,
    /** Completely random order. */
    RANDOM,
    /** Random, but the two pairs of a face are kept far apart -> more traps. */
    SPREAD,
}

enum class Difficulty(
    /** Maximum number of hints, `null` = unlimited. */
    val maxHints: Int?,
    /** Score multiplier in percent. */
    val scorePercent: Int,
    /** Par time in seconds for a 144-tile board (scaled by tile count). */
    val parSecondsFor144: Int,
    /** Time bonus points per second below par. */
    val timeBonusPerSecond: Int,
    val pairOrdering: PairOrdering,
    /** Prefer faces that appear only twice on the board (easier). */
    val preferSinglePairs: Boolean,
    /** Number of candidate boards generated; the one closest to the target is chosen. */
    val candidates: Int,
    /** true = choose candidate with the most initially free pairs, false = the fewest. */
    val maximizeFreePairs: Boolean,
) {
    EASY(null, 100, 420, 2, PairOrdering.GROUPED, true, 4, true),
    MEDIUM(null, 125, 600, 3, PairOrdering.MIXED, false, 2, true),
    HARD(5, 150, 780, 4, PairOrdering.RANDOM, false, 3, false),
    EXPERT(3, 200, 900, 5, PairOrdering.SPREAD, false, 5, false);
}
