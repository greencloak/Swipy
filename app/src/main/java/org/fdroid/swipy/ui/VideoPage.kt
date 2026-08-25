package org.fdroid.swipy.ui

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import org.fdroid.swipy.data.MediaItem

@Composable
fun VideoPage(item: MediaItem, isActive: Boolean) {
    val context = LocalContext.current

    val player = remember(item.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(item.uri))
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
            prepare()
        }
    }

    // Play only the currently visible page; pause/release others to save battery.
    LaunchedEffect(isActive) {
        if (isActive) player.play() else player.pause()
    }

    DisposableEffect(item.uri) {
        onDispose { player.release() }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            PlayerView(context).apply {
                useController = true
                this.player = player
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }
    )
}
