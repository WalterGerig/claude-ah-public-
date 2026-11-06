package com.mahjongzen.ui.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** A removed tile that flies towards its partner and fades out. */
class FlyingTile(val slot: Int, val partner: Int, val face: Int, val start: Long)

/** Spark particle in board coordinates (half-tile units). Position is computed analytically. */
class Particle(
    val x0: Float, val y0: Float, val vx: Float, val vy: Float,
    val color: Color, val start: Long, val life: Long, val size: Float,
)

/**
 * Short-lived visual effects of the board. Mutated from the UI thread only; [version]
 * is snapshot state so the frame loop restarts whenever something new is added.
 */
class BoardEffects {
    var version by mutableIntStateOf(0)
        private set

    val flying = ArrayList<FlyingTile>()
    val particles = ArrayList<Particle>()
    var invalidTile = -1
        private set
    var invalidAt = 0L
        private set
    var selectAt = 0L
        private set
    var selectedTile = -1
        private set

    private val rng = Random(System.nanoTime())

    fun onSelect(tile: Int, now: Long) {
        selectedTile = tile
        selectAt = now
        version++
    }

    fun onInvalid(tile: Int, now: Long) {
        invalidTile = tile
        invalidAt = now
        version++
    }

    fun onRemoved(a: Int, b: Int, faceA: Int, faceB: Int, ax: Float, ay: Float, bx: Float, by: Float, combo: Int, now: Long, reduced: Boolean = false) {
        flying += FlyingTile(a, b, faceA, now)
        flying += FlyingTile(b, a, faceB, now)
        if (!reduced) {
            val cx = (ax + bx) / 2 + 1f
            val cy = (ay + by) / 2 + 1f
            val count = 6 + combo.coerceAtMost(6) * 5
            val palette = if (combo >= 2) listOf(Color(0xFFFFD36B), Color(0xFFFFB347), Color(0xFFFFF4C2), Color(0xFFE8735A))
            else listOf(Color(0xFFFFF4D6), Color(0xFFE9DDBF))
            repeat(count) {
                val ang = rng.nextDouble(0.0, 2 * PI)
                val speed = rng.nextDouble(2.0, 6.0 + combo).toFloat()
                particles += Particle(
                    cx, cy, cos(ang).toFloat() * speed, sin(ang).toFloat() * speed - 2f,
                    palette[rng.nextInt(palette.size)], now, rng.nextLong(450_000_000L, 900_000_000L), rng.nextDouble(0.08, 0.2).toFloat(),
                )
            }
        }
        version++
    }

    fun isActive(now: Long): Boolean {
        flying.removeAll { now - it.start > FLY_NANOS }
        particles.removeAll { now - it.start > it.life }
        return flying.isNotEmpty() || particles.isNotEmpty() ||
            now - invalidAt < SHAKE_NANOS || now - selectAt < LIFT_NANOS
    }

    companion object {
        const val FLY_NANOS = 420_000_000L
        const val SHAKE_NANOS = 350_000_000L
        const val LIFT_NANOS = 140_000_000L
        const val GRAVITY = 9f // half units / s²
    }
}
