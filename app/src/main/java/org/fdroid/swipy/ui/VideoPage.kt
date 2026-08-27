package org.fdroid.swipy.ui

import android.view.ViewGroup
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Settings
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
import org.fdroid.swipy.data.LikedMediaStore
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
    likedStore: LikedMediaStore,
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
                        val wasPlaying = isPlaying
                        if (wasPlaying) {
                            player.pause()
                            isPlaying = false
                        }
                        // Always resume once the touch ends, whether released cleanly
                        // or the gesture turned into a swipe to the next video.
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

            val isLiked = likedStore.isLiked(item.id)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
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
            }

            if (durationMs > 0) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    GlassSeekBar(
                        positionMs = positionMs,
                        durationMs = durationMs,
                        onSeekChange = {
                            isUserSeeking = true
                            positionMs = it
                        },
                        onSeekFinished = {
                            player.seekTo(it)
                            isUserSeeking = false
                        }
                    )
                }
            }
        }
    }
}
