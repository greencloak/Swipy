package org.fdroid.swipy.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.fdroid.swipy.data.LikedMediaStore
import org.fdroid.swipy.data.MediaItem

/** Two taps within this window count as a double-tap-to-zoom, not two single taps. */
private const val DOUBLE_TAP_WINDOW_MS = 300L

@Composable
fun ImagePage(
    item: MediaItem,
    likedStore: LikedMediaStore,
    onShuffleAndRandomStart: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var showControls by remember(item.uri) { mutableStateOf(false) }
    val zoomState = remember(item.uri) { ZoomPanState() }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var lastTapTime by remember(item.uri) { mutableStateOf(0L) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(item.uri) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var moved = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break

                        if (pressed.size >= 2) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            zoomState.onTransform(containerSize, panChange, zoomChange)
                            event.changes.forEach { it.consume() }
                            moved = true
                        } else if (zoomState.isZoomed) {
                            // Single finger, but already zoomed in: pan the
                            // image instead of letting the pager treat this
                            // as a page-swipe.
                            val change = pressed.first()
                            val delta = change.positionChange()
                            if (delta != Offset.Zero) {
                                zoomState.onPan(containerSize, delta)
                                change.consume()
                                moved = true
                            }
                        }
                        // Single finger, not zoomed: leave completely
                        // unconsumed so the VerticalPager sees the swipe.
                    }

                    if (!moved) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime < DOUBLE_TAP_WINDOW_MS) {
                            zoomState.onDoubleTap()
                            lastTapTime = 0L
                        } else {
                            showControls = !showControls
                            lastTapTime = now
                        }
                    }
                }
            }
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoomState.scale
                    scaleY = zoomState.scale
                    translationX = zoomState.offsetX
                    translationY = zoomState.offsetY
                }
        )

        if (showControls) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
            ) {
                GlassIconButton(
                    icon = Icons.Default.Refresh,
                    contentDescription = "Refresh library",
                    onClick = onRefresh
                )
                GlassIconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = "Settings",
                    onClick = onOpenSettings
                )
            }

            val isLiked = likedStore.isLiked(item.id)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
            ) {
                GlassIconButton(
                    icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isLiked) "Unlike" else "Like",
                    tint = if (isLiked) Color(0xFFE0245E) else Color.White,
                    onClick = { likedStore.toggle(item.id) }
                )
                GlassIconButton(
                    icon = Icons.Default.Shuffle,
                    contentDescription = "Shuffle and jump to a random item",
                    onClick = onShuffleAndRandomStart
                )
            }
        }
    }
}
