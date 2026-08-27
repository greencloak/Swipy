package org.fdroid.swipy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp

/**
 * A minimal seek bar with no visible thumb — just a mostly-solid (~90%
 * opaque) filled track. The tappable/draggable zone is taller than the
 * visible bar so it's easy to grab without the bar itself looking chunky.
 * Shows a floating time readout above the bar while actively scrubbing.
 */
@Composable
fun GlassSeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeekChange: (Long) -> Unit,
    onSeekFinished: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var barWidthPx by remember { mutableStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableStateOf(positionMs) }

    val displayedPosition = if (isSeeking) scrubPositionMs else positionMs
    val fraction = if (durationMs > 0) (displayedPosition.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Box(modifier = modifier.fillMaxWidth()) {
        if (isSeeking) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(bottom = 32.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    formatSeekTime(scrubPositionMs),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Taller invisible zone (28dp) makes the bar much easier to grab with
        // a finger than the 4dp visible track alone would allow.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .onSizeChanged { barWidthPx = it.width.toFloat() }
                .pointerInput(durationMs) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isSeeking = true
                        if (barWidthPx > 0 && durationMs > 0) {
                            val frac = (down.position.x / barWidthPx).coerceIn(0f, 1f)
                            scrubPositionMs = (frac * durationMs).toLong()
                            onSeekChange(scrubPositionMs)
                        }
                        var lastChangeId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == lastChangeId } ?: break
                            if (!change.pressed) break
                            change.consume()
                            if (barWidthPx > 0 && durationMs > 0) {
                                val frac = (change.position.x / barWidthPx).coerceIn(0f, 1f)
                                scrubPositionMs = (frac * durationMs).toLong()
                                onSeekChange(scrubPositionMs)
                            }
                            lastChangeId = change.id
                        }
                        isSeeking = false
                        onSeekFinished(scrubPositionMs)
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // Empty (unfilled) portion of the track.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
            )
            // Filled portion — ~90% opaque so it reads as mostly solid.
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.90f), RoundedCornerShape(2.dp))
            )
        }
    }
}

private fun formatSeekTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
