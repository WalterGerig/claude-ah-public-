package com.mahjongzen.ui.game

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.abs

/**
 * Tap + pinch/pan in ONE detector, without double-tap support.
 *
 * `detectTapGestures(onDoubleTap = …)` delays every single tap by the double-tap timeout
 * (~300 ms) because it has to wait whether a second tap follows. Here a tap is reported the
 * moment the finger is lifted, as long as it stayed within the touch slop and no second finger
 * was involved. Anything else becomes a transform (zoom / pan) and never produces a tap.
 */
suspend fun PointerInputScope.detectBoardGestures(
    onTap: (Offset) -> Unit,
    onTransform: (centroid: Offset, pan: Offset, zoom: Float) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val slop = viewConfiguration.touchSlop
        var transforming = false
        var cancelled = false
        var totalPan = Offset.Zero
        var totalZoom = 1f
        while (true) {
            val event = awaitPointerEvent()
            if (event.changes.any { it.isConsumed }) { cancelled = true; break }
            val pressed = event.changes.count { it.pressed }
            if (pressed == 0) break
            val zoomChange = event.calculateZoom()
            val panChange = event.calculatePan()
            if (!transforming) {
                totalZoom *= zoomChange
                totalPan += panChange
                val zoomMotion = abs(1f - totalZoom) * event.calculateCentroidSize(useCurrent = false)
                if (pressed > 1 || zoomMotion > slop || totalPan.getDistance() > slop) transforming = true
            }
            if (transforming) {
                if (zoomChange != 1f || panChange != Offset.Zero) {
                    onTransform(event.calculateCentroid(useCurrent = false), panChange, zoomChange)
                }
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        }
        if (!transforming && !cancelled) onTap(down.position)
    }
}
