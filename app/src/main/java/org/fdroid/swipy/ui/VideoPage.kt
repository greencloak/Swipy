package org.fdroid.swipy.ui

import android.view.ViewGroup
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import org.fdroid.swipy.data.LikedMediaStore
import org.fdroid.swipy.data.MediaItem
import org.fdroid.swipy.data.PlaybackPositionStore

/** Touch must be held this long before it counts as "hold to pause" — a quick
 *  tap (to open the overlay) never pauses the video at all. */
private const val HOLD_TO_PAUSE_THRESHOLD_MS = 150L

@Composable
fun VideoPage(
    item: MediaItem,
    isActive: Boolean,
    loopEnabled: Boolean,
    startMidwayEnabled: Boolean,
    rememberPositionEnabled: Boolean,
    autoAdvanceEnabled: Boolean,
    positionStore: PlaybackPositionStore,
    likedStore: LikedMediaStore,
    randomStartItemId: Long?,
    onRandomStartConsumed: () -> Unit,
    onShuffleAndRandomStart: () -> Unit,
    onVideoEnded: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current

    val player = remember(item.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(item.uri))
            repeatMode = if (loopEnabled) ExoPlayer.REPEAT_MODE_ONE else ExoPlayer.REPEAT_MODE_OFF
            prepare()
        }
    }

    LaunchedEffect(loopEnabled) {
        player.repeatMode = if (loopEnabled) ExoPlayer.REPEAT_MODE_ONE else ExoPlayer.REPEAT_MODE_OFF
    }

    var isPlaying by remember(item.uri) { mutableStateOf(true) }
    var showControls by remember(item.uri) { mutableStateOf(false) }

    var durationMs by remember(item.uri) { mutableStateOf(0L) }
    var positionMs by remember(item.uri) { mutableStateOf(0L) }
    var isUserSeeking by remember { mutableStateOf(false) }

    LaunchedEffect(isActive) {
        if (isActive) {
            player.play()
            isPlaying = true
        } else {
            player.pause()
            isPlaying = false
        }
    }

    var hasAppliedInitialSeek by remember(item.uri) { mutableStateOf(false) }
    DisposableEffect(item.uri) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY && !hasAppliedInitialSeek) {
                    hasAppliedInitialSeek = true
                    if (item.id == randomStartItemId) {
                        val dur = player.duration
                        if (dur > 0) {
                            player.seekTo((dur * kotlin.random.Random.nextFloat()).toLong())
                        }
                        onRandomStartConsumed()
                    } else {
                        val saved = if (rememberPositionEnabled) positionStore.getPosition(item.id) else 0L
                        if (rememberPositionEnabled && saved > 0) {
                            player.seekTo(saved)
                        } else if (startMidwayEnabled) {
                            val dur = player.duration
                            if (dur > 0) player.seekTo(dur / 2)
                        }
                    }
                } else if (state == Player.STATE_ENDED && autoAdvanceEnabled) {
                    onVideoEnded()
                }
            }
        }
        player.addListener(listener)
        onDispose {
            if (rememberPositionEnabled) {
                positionStore.savePosition(item.id, player.currentPosition)
            }
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(item.uri) {
        while (true) {
            if (!isUserSeeking) {
                val dur = player.duration
                if (dur > 0) durationMs = dur
                positionMs = player.currentPosition.coerceIn(0L, if (durationMs > 0) durationMs else Long.MAX_VALUE)
            }
            delay(300)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(item.uri) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var pausedForHold = false
                    val wasPlayingAtStart = isPlaying

                    // null = held past the threshold without releasing or being
                    // claimed elsewhere; true = genuine quick release (a tap);
                    // false = the gesture was consumed by something else (the
                    // dedicated bottom scrub zone claiming a drag) — in that
                    // case we do nothing at all, since the seek bar is handling it.
                    val outcome: Boolean? = try {
                        withTimeout(HOLD_TO_PAUSE_THRESHOLD_MS) {
                            awaitReleaseOrConsumption(down.id)
                        }
                    } catch (e: TimeoutCancellationException) {
                        null
                    }

                    when (outcome) {
                        true -> showControls = !showControls
                        false -> { /* claimed by the seek bar's scrub zone — ignore */ }
                        null -> {
                            if (wasPlayingAtStart) {
                                player.pause()
                                isPlaying = false
                                pausedForHold = true
                            }
                            awaitReleaseOrConsumption(down.id)
                            if (pausedForHold) {
                                player.play()
                                isPlaying = true
                            }
                        }
                    }
                }
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PlayerView(context).apply {
                    useController = false
                    this.player = player
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            }
        )

        if (showControls) {
            GlassIconButton(
                icon = Icons.Default.Settings,
                contentDescription = "Settings",
                onClick = onOpenSettings,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
            )
        }

        val isLiked = likedStore.isLiked(item.id)
        if (showControls) {
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
                    contentDescription = "Shuffle and jump to a random point",
                    onClick = onShuffleAndRandomStart
                )
            }
        }

        // Bottom ~13% of the video is always an active scrub zone, whether or
        // not the rest of the overlay is open.
        GlassSeekBar(
            positionMs = positionMs,
            durationMs = durationMs,
            visible = showControls,
            onSeekChange = {
                isUserSeeking = true
                positionMs = it
            },
            onSeekFinished = {
                player.seekTo(it)
                isUserSeeking = false
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.13f)
        )
    }
}

/** Waits for release; returns true on a genuine release, false if the
 *  gesture was consumed elsewhere first (e.g. claimed by the seek bar). */
private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.awaitReleaseOrConsumption(
    pointerId: androidx.compose.ui.input.pointer.PointerId
): Boolean {
    var id = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == id } ?: return true
        if (change.isConsumed) return false
        if (!change.pressed) {
            change.consume()
            return true
        }
        id = change.id
    }
}
