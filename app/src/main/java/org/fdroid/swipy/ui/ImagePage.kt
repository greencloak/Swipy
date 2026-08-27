package org.fdroid.swipy.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.fdroid.swipy.data.LikedMediaStore
import org.fdroid.swipy.data.MediaItem

@Composable
fun ImagePage(
    item: MediaItem,
    likedStore: LikedMediaStore,
    onOpenSettings: () -> Unit
) {
    var showControls by remember(item.uri) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { showControls = !showControls }
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        if (showControls) {
            GlassIconButton(
                icon = Icons.Default.Settings,
                contentDescription = "Settings",
                onClick = onOpenSettings,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
            )

            val isLiked = likedStore.isLiked(item.id)
            GlassIconButton(
                icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isLiked) "Unlike" else "Like",
                tint = if (isLiked) Color(0xFFE0245E) else Color.White,
                onClick = { likedStore.toggle(item.id) },
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)
            )
        }
    }
}
