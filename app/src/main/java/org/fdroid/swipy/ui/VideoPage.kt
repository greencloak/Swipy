package org.fdroid.swipy.ui

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import org.fdroid.swipy.data.MediaItem
import org.fdroid.swipy.data.PlaybackPositionStore
import org.fdroid.swipy.data.PlaybackStartMode

@Composable
fun VideoPage(
    item: MediaItem,
    isActive: Boolean,
    loopEnabled: Boolean,
    playbackStartMode: PlaybackStartMode,
    positionStore: PlaybackPositionStore,
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

    // Keep repeat mode in sync if the user changes the Loop setting mid-playback.
    LaunchedEffect(loopEnabled) {
        player.repeatMode = if (loopEnabled) ExoPlayer.REPEAT_MODE_ONE else ExoPlayer.REPEAT_MODE_OFF
    }

    var isPlaying by remember(item.uri) { mutableStateOf(true) }
    // Settings gear + seek bar hidden by default; only appear on single tap.
    var showControls by remember(item.uri) { mutableStateOf(false) }

    // Seek bar state
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

    // Apply "start midway" / "remember position" exactly once, as soon as the
    // player knows its duration (needed for the midway calculation).
    var hasAppliedInitialSeek by remember(item.uri) { mutableStateOf(false) }
    DisposableEffect(item.uri) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY && !hasAppliedInitialSeek) {
                    hasAppliedInitialSeek = true
                    when (playbackStartMode) {
                        PlaybackStartMode.START_MIDWAY -> {
                            val dur = player.duration
                            if (dur > 0) player.seekTo(dur / 2)
                        }
                        PlaybackStartMode.REMEMBER_POSITION -> {
                            val saved = positionStore.getPosition(item.id)
                            if (saved > 0) player.seekTo(saved)
                        }
                        PlaybackStartMode.DEFAULT -> {}
                    }
                }
            }
        }
        player.addListener(listener)
        onDispose {
            if (playbackStartMode == PlaybackStartMode.REMEMBER_POSITION) {
                positionStore.savePosition(item.id, player.currentPosition)
            }
            player.removeListener(listener)
            player.release()
        }
    }

    // Poll playback position/duration so the seek bar stays in sync.
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
                detectTapGestures(
                    onPress = {
                        // Hold to pause, release to resume — the only way to pause a video.
                        val wasPlaying = isPlaying
                        if (wasPlaying) {
                            player.pause()
                            isPlaying = false
                        }
                        // Always resume once the touch interaction ends, whether it was
                        // a clean release OR got cancelled (e.g. the gesture turned into
                        // a swipe to the next video). Previously we only resumed on a
                        // clean release, which left the video stuck paused after swiping.
                        tryAwaitRelease()
                        if (wasPlaying) {
                            player.play()
                            isPlaying = true
                        }
                    },
                    onTap = {
                        showControls = !showControls
                    }
                )
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PlayerView(context).apply {
                    // Disable ExoPlayer's built-in controller entirely; we draw our own.
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
            // No background scrim on purpose — icons float directly over the video.
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
            }

            // Seek bar, pinned to the bottom, no background dimming behind it.
            if (durationMs > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        formatTime(positionMs),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Slider(
                        value = positionMs.toFloat(),
                        valueRange = 0f..durationMs.toFloat(),
                        onValueChange = {
                            isUserSeeking = true
                            positionMs = it.toLong()
                        },
                        onValueChangeFinished = {
                            player.seekTo(positionMs)
                            isUserSeeking = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    Text(
                        formatTime(durationMs),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
