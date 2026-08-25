package org.fdroid.swipy.ui

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import org.fdroid.swipy.data.MediaItem
import org.fdroid.swipy.data.SortOrder

@Composable
fun VideoPage(
    item: MediaItem,
    isActive: Boolean,
    onOpenFolderPicker: () -> Unit,
    onShuffleNow: () -> Unit,
    onSortChange: (SortOrder) -> Unit
) {
    val context = LocalContext.current

    val player = remember(item.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(item.uri))
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
            prepare()
        }
    }

    // Track play/pause state so our custom icon reflects reality.
    var isPlaying by remember(item.uri) { mutableStateOf(true) }
    // Controls hidden by default; only appear when the user taps the video.
    var showControls by remember(item.uri) { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }

    LaunchedEffect(isActive) {
        if (isActive) {
            player.play()
            isPlaying = true
        } else {
            player.pause()
            isPlaying = false
        }
    }

    DisposableEffect(item.uri) {
        onDispose { player.release() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { showControls = !showControls }
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
                onClick = {
                    if (isPlaying) player.pause() else player.play()
                    isPlaying = !isPlaying
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp)
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                IconButton(
                    onClick = { showSettingsMenu = true },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                }
                SettingsMenu(
                    expanded = showSettingsMenu,
                    onDismiss = { showSettingsMenu = false },
                    onOpenFolderPicker = onOpenFolderPicker,
                    onShuffleNow = onShuffleNow,
                    onSortChange = onSortChange
                )
            }
        }
    }
}
