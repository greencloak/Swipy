package org.fdroid.swipy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.fdroid.swipy.data.MediaItem
import org.fdroid.swipy.data.SortOrder

@Composable
fun ImagePage(
    item: MediaItem,
    onOpenFolderPicker: () -> Unit,
    onShuffleNow: () -> Unit,
    onSortChange: (SortOrder) -> Unit
) {
    var showControls by remember(item.uri) { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }

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
