package org.fdroid.swipy.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/**
 * A minimal seek bar with no visible thumb — just a mostly-solid (~90%
 * opaque) filled track. The bottom ~13% of the video is always an active
 * drag/tap zone for seeking, whether or not the rest of the controls are
 * showing, so scrubbing doesn't require opening the overlay first. While
 * actively scrubbing, the bar rises 5% of the screen height (matching
 * Instagram's reel scrubber) so it isn't hidden under your thumb.
 */
@Composable
fun GlassSeekBar(
    positionMs: Long,
    durationMs: Long,
    visible: Boolean,
    onSeekChange: (Long) -> Unit,
    onSeekFinished: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val riseDistance = screenHeightDp * 0.05f

    var barWidthPx by remember { mutableStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }
    var lastComputedMs by remember { mutableStateOf(positionMs) }

    val offsetY by animateDpAsState(if (isSeeking) -riseDistance else 0.dp, label = "seekBarRise")
    val displayedPosition = if (isSeeking) lastComputedMs else positionMs
    val fraction = if (durationMs > 0) (displayedPosition.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .onSizeChanged { barWidthPx = it.width.toFloat() }
            .pointerInput(durationMs) {
                if (durationMs <= 0) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isSeeking = true
                    if (barWidthPx > 0) {
                        val frac = (down.position.x / barWidthPx).coerceIn(0f, 1f)
                        lastComputedMs = (frac * durationMs).toLong()
                        onSeekChange(lastComputedMs)
                    }
                    var pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) break
                        change.consume()
                        if (barWidthPx > 0) {
                            val frac = (change.position.x / barWidthPx).coerceIn(0f, 1f)
                            lastComputedMs = (frac * durationMs).toLong()
                            onSeekChange(lastComputedMs)
                        }
                        pointerId = change.id
                    }
                    isSeeking = false
                    onSeekFinished(lastComputedMs)
                }
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        if (visible || isSeeking) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 10.dp)
                    .offset(y = offsetY)
                    .height(4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
                )
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.90f), RoundedCornerShape(2.dp))
                )
            }
        }
    }
}
