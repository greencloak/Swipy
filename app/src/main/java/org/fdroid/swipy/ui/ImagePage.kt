package org.fdroid.swipy.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import org.fdroid.swipy.data.MediaItem

@Composable
fun ImagePage(item: MediaItem) {
    AsyncImage(
        model = item.uri,
        contentDescription = item.displayName,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize()
    )
}
