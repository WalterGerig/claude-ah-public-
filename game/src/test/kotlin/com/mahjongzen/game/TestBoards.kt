package com.mahjongzen.game

import com.mahjongzen.game.model.Layout
import com.mahjongzen.game.model.Slot
import com.mahjongzen.game.model.TileFace

/** Helpers to build tiny hand-made boards for rule tests. */
object TestBoards {
    fun layoutOf(vararg slots: Slot) = Layout(slots.toList())

    fun faces(vararg f: TileFace): IntArray = f.map { it.ordinal }.toIntArray()
}
