package org.fdroid.swipy.ui

import android.graphics.Bitmap
import android.os.Build
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fdroid.swipy.data.MediaItem
import org.fdroid.swipy.data.MediaType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikedGalleryScreen(
    likedItems: List<MediaItem>,
    onItemSelected: (MediaItem) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Liked") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (likedItems.isEmpty()) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Nothing liked yet. Tap the heart on a photo or video to save it here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(2.dp)
            ) {
                items(likedItems, key = { it.id }) { item ->
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .aspectRatio(1f)
                            .background(Color.DarkGray)
                            .clickable { onItemSelected(item) }
                    ) {
                        if (item.type == MediaType.IMAGE) {
                            AsyncImage(
                                model = item.uri,
                                contentDescription = item.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            VideoThumbnail(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoThumbnail(item: MediaItem) {
    val context = LocalContext.current
    var thumbnail by remember(item.uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(item.uri) {
        thumbnail = withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(item.uri, Size(300, 300), null)
                } else {
                    null
                }
            }.getOrNull()
        }
    }

    val bmp = thumbnail
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
    // Play icon always shown on top so videos are recognizable even before
    // (or if) the thumbnail loads — matches the pattern used elsewhere.
    Icon(
        Icons.Default.PlayArrow,
        contentDescription = "Video",
        tint = Color.White,
        modifier = Modifier.align(Alignment.Center).size(28.dp)
    )
}
