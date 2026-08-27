package org.fdroid.swipy.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.fdroid.swipy.data.LikedMediaStore
import org.fdroid.swipy.data.MediaItem

private const val DOUBLE_TAP_WINDOW_MS = 250L

@Composable
fun ImagePage(
    item: MediaItem,
    likedStore: LikedMediaStore,
    onShuffleAndRandomStart: () -> Unit,
    onOpenSettings: () -> Unit
) {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(item.uri) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val released = awaitReleaseSimple()
                    if (!released) return@awaitEachGesture
                    val gotSecondTap = try {
                        withTimeout(DOUBLE_TAP_WINDOW_MS) { awaitSecondTapDownSimple() }
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
            }
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
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

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.awaitReleaseSimple(): Boolean {
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull() ?: return false
        if (!change.pressed) {
            change.consume()
            return true
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.awaitSecondTapDownSimple(): Boolean {
    while (true) {
        val event = awaitPointerEvent()
        val newDown = event.changes.firstOrNull { it.pressed && !it.previousPressed }
        if (newDown != null) {
            newDown.consume()
            return true
        }
    }
}
