package org.fdroid.swipy.ui

import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
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

/** How long after a single tap we wait to see if a second tap follows,
 *  before committing to "single tap" behavior (open the overlay). */
private const val DOUBLE_TAP_WINDOW_MS = 250L

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
    var showHeartBurst by remember { mutableStateOf(false) }
    var heartBurstTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(heartBurstTrigger) {
        if (heartBurstTrigger > 0) {
            showHeartBurst = true
            delay(650)
            showHeartBurst = false
        }
    }

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
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startTime = System.currentTimeMillis()
                    var pointerId = down.id

                    // Race: quick release (tap), movement beyond slop (a swipe
                    // — bail out entirely so the pager handles it), or the
                    // threshold elapsing with no release/movement (a hold).
                    var outcome = "hold"
                    while (true) {
                        val elapsed = System.currentTimeMillis() - startTime
                        val remaining = HOLD_TO_PAUSE_THRESHOLD_MS - elapsed
                        if (remaining <= 0) {
                            outcome = "hold"
                            break
                        }
                        val event = try {
                            withTimeout(remaining) { awaitPointerEvent() }
                        } catch (e: TimeoutCancellationException) {
                            outcome = "hold"
                            break
                        }
                        val change = event.changes.firstOrNull { it.id == pointerId }
                        if (change == null) {
                            outcome = "swipe"
                            break
                        }
                        if (change.isConsumed) {
                            outcome = "consumed"
                            break
                        }
                        if (!change.pressed) {
                            change.consume()
                            outcome = "tap"
                            break
                        }
                        val dx = change.position.x - down.position.x
                        val dy = change.position.y - down.position.y
                        if (dx * dx + dy * dy > slop * slop) {
                            outcome = "swipe"
                            break
                        }
                        pointerId = change.id
                    }

                    when (outcome) {
                        "tap" -> {
                            // Wait briefly for a possible second tap (double-tap
                            // to like) before committing to "single tap opens
                            // the overlay".
                            val gotSecondTap = try {
                                withTimeout(DOUBLE_TAP_WINDOW_MS) { awaitSecondTapDown() }
                            } catch (e: TimeoutCancellationException) {
                                false
                            }
                            if (gotSecondTap) {
                                likedStore.toggle(item.id)
                                heartBurstTrigger++
                            } else {
                                showControls = !showControls
                            }
                        }
                        "swipe", "consumed" -> { /* let the pager / seek bar handle it entirely */ }
                        "hold" -> {
                            // Query the player directly rather than trusting our
                            // cached `isPlaying` — that value could occasionally
                            // drift from reality (e.g. right as a video ends or
                            // the pager settles), which was causing hold-to-pause
                            // to silently do nothing on some attempts.
                            val wasActuallyPlaying = player.isPlaying
                            if (wasActuallyPlaying) {
                                player.pause()
                                isPlaying = false
                            }
                            awaitReleaseOrConsumption(pointerId)
                            if (wasActuallyPlaying) {
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

        AnimatedVisibility(
            visible = showHeartBurst,
            enter = scaleIn(animationSpec = tween(200)) + fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(110.dp)
            )
        }

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
                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    onClick = {
                        if (isPlaying) {
                            player.pause()
                            isPlaying = false
                        } else {
                            player.play()
                            isPlaying = true
                        }
                    }
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

/** Watches for a brand-new finger touching down (a second tap) and consumes it. */
private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.awaitSecondTapDown(): Boolean {
    while (true) {
        val event = awaitPointerEvent()
        val newDown = event.changes.firstOrNull { it.pressed && !it.previousPressed }
        if (newDown != null) {
            newDown.consume()
            return true
        }
    }
}
