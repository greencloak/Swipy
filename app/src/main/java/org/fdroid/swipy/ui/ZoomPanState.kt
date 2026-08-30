package org.fdroid.swipy.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.max

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f

/**
 * Pinch-to-zoom / pan state for a single media item (photo or video),
 * following the same "fit by default, zoom to fill" pattern used by gallery
 * apps like Fossify Gallery: content stays fully visible (letterboxed)
 * until the user pinches or double-taps, at which point it scales up and
 * can be panned around — always clamped so it can never be dragged fully
 * off screen. Applied purely as a scale + translate pair via
 * Modifier.graphicsLayer, so it never touches how the image is decoded or
 * how the video surface is laid out underneath.
 *
 * Create one per item with `remember(item.uri) { ZoomPanState() }` so it
 * resets automatically when the pager moves to a different item.
 */
class ZoomPanState {
    var scale by mutableFloatStateOf(MIN_SCALE)
        private set
    var offsetX by mutableFloatStateOf(0f)
        private set
    var offsetY by mutableFloatStateOf(0f)
        private set

    val isZoomed: Boolean get() = scale > 1.01f

    /** One frame of a two-finger pinch/pan gesture. */
    fun onTransform(containerSize: IntSize, pan: Offset, zoom: Float) {
        scale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
        offsetX += pan.x
        offsetY += pan.y
        clampOffset(containerSize)
    }

    /** One frame of a single-finger drag — only meaningful once zoomed in. */
    fun onPan(containerSize: IntSize, pan: Offset) {
        offsetX += pan.x
        offsetY += pan.y
        clampOffset(containerSize)
    }

    /** Toggles between fit (1x) and a fixed zoomed-in level. */
    fun onDoubleTap() {
        if (isZoomed) reset() else scale = DOUBLE_TAP_SCALE
    }

    fun reset() {
        scale = MIN_SCALE
        offsetX = 0f
        offsetY = 0f
    }

    private fun clampOffset(containerSize: IntSize) {
        // At scale s, content overhangs the container by (s - 1) * size / 2
        // on each side — panning past that would reveal empty space.
        val maxX = max(0f, (scale - 1f) * containerSize.width / 2f)
        val maxY = max(0f, (scale - 1f) * containerSize.height / 2f)
        offsetX = offsetX.coerceIn(-maxX, maxX)
        offsetY = offsetY.coerceIn(-maxY, maxY)
    }
}
