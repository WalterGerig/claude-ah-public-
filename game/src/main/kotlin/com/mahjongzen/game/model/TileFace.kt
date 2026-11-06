package com.mahjongzen.game.model

/** Suit / family of a tile. */
enum class Suit { DOTS, BAMBOO, CHARACTERS, WIND, DRAGON, FLOWER, SEASON }

/**
 * All 42 distinct tile faces of a 144-tile Mahjong set.
 *
 * 34 faces exist four times each (suits 1–9, winds, dragons). The four flowers and the
 * four seasons exist once each, but any flower matches any flower and any season matches
 * any season – so there are exactly 36 "match groups" with 4 tiles each.
 */
enum class TileFace(val suit: Suit, val rank: Int) {
    DOT_1(Suit.DOTS, 1), DOT_2(Suit.DOTS, 2), DOT_3(Suit.DOTS, 3),
    DOT_4(Suit.DOTS, 4), DOT_5(Suit.DOTS, 5), DOT_6(Suit.DOTS, 6),
    DOT_7(Suit.DOTS, 7), DOT_8(Suit.DOTS, 8), DOT_9(Suit.DOTS, 9),

    BAMBOO_1(Suit.BAMBOO, 1), BAMBOO_2(Suit.BAMBOO, 2), BAMBOO_3(Suit.BAMBOO, 3),
    BAMBOO_4(Suit.BAMBOO, 4), BAMBOO_5(Suit.BAMBOO, 5), BAMBOO_6(Suit.BAMBOO, 6),
    BAMBOO_7(Suit.BAMBOO, 7), BAMBOO_8(Suit.BAMBOO, 8), BAMBOO_9(Suit.BAMBOO, 9),

    CHAR_1(Suit.CHARACTERS, 1), CHAR_2(Suit.CHARACTERS, 2), CHAR_3(Suit.CHARACTERS, 3),
    CHAR_4(Suit.CHARACTERS, 4), CHAR_5(Suit.CHARACTERS, 5), CHAR_6(Suit.CHARACTERS, 6),
    CHAR_7(Suit.CHARACTERS, 7), CHAR_8(Suit.CHARACTERS, 8), CHAR_9(Suit.CHARACTERS, 9),

    WIND_EAST(Suit.WIND, 1), WIND_SOUTH(Suit.WIND, 2), WIND_WEST(Suit.WIND, 3), WIND_NORTH(Suit.WIND, 4),

    DRAGON_RED(Suit.DRAGON, 1), DRAGON_GREEN(Suit.DRAGON, 2), DRAGON_WHITE(Suit.DRAGON, 3),

    FLOWER_PLUM(Suit.FLOWER, 1), FLOWER_ORCHID(Suit.FLOWER, 2),
    FLOWER_CHRYSANTHEMUM(Suit.FLOWER, 3), FLOWER_BAMBOO(Suit.FLOWER, 4),

    SEASON_SPRING(Suit.SEASON, 1), SEASON_SUMMER(Suit.SEASON, 2),
    SEASON_AUTUMN(Suit.SEASON, 3), SEASON_WINTER(Suit.SEASON, 4);

    /** Tiles with the same match group can be paired. */
    val matchGroup: Int
        get() = when (suit) {
            Suit.FLOWER -> FLOWER_GROUP
            Suit.SEASON -> SEASON_GROUP
            else -> ordinal
        }

    /** Dragons, winds, flowers and seasons give bonus points. */
    val isRare: Boolean
        get() = suit == Suit.WIND || suit == Suit.DRAGON || suit == Suit.FLOWER || suit == Suit.SEASON

    fun matches(other: TileFace): Boolean = matchGroup == other.matchGroup

    companion object {
        const val FLOWER_GROUP = 34
        const val SEASON_GROUP = 35
        const val GROUP_COUNT = 36

        private val values = entries.toTypedArray()
        fun of(ordinal: Int): TileFace = values[ordinal]

        /** All faces belonging to a match group (1 face for normal groups, 4 for flowers/seasons). */
        fun facesOfGroup(group: Int): List<TileFace> = when (group) {
            FLOWER_GROUP -> entries.filter { it.suit == Suit.FLOWER }
            SEASON_GROUP -> entries.filter { it.suit == Suit.SEASON }
            else -> listOf(values[group])
        }

        /** The full classic 144-tile set. */
        fun fullSet(): List<TileFace> = (0 until GROUP_COUNT).flatMap { g ->
            val faces = facesOfGroup(g)
            if (faces.size == 1) List(4) { faces[0] } else faces
        }
    }
}
