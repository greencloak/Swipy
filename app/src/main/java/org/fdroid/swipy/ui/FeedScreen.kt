package org.fdroid.swipy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.fdroid.swipy.data.MediaItem
import org.fdroid.swipy.data.MediaType
import org.fdroid.swipy.data.SortOrder

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    items: List<MediaItem>,
    sortOrder: SortOrder,
    onSortChange: (SortOrder) -> Unit,
    onOpenFolderPicker: () -> Unit,
    onShuffleNow: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { items.size })
    var showSortMenu by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (items.isEmpty()) {
            Text(
                "No media found. Try selecting different folders.",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center).padding(24.dp)
            )
        } else {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                MediaPage(item = items[page], isActive = pagerState.currentPage == page)
            }
        }

        // Top control bar
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPaddingCompat()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalIconButton(onClick = onOpenFolderPicker) {
                Icon(Icons.Default.FolderOpen, contentDescription = "Select folders")
            }
            Box {
                FilledTonalIconButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort")
                }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    SortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.label) },
                            onClick = {
                                onSortChange(order)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
            FilledTonalIconButton(onClick = onShuffleNow) {
                Icon(Icons.Default.Shuffle, contentDescription = "Shuffle now")
            }
        }
    }
}

@Composable
private fun MediaPage(item: MediaItem, isActive: Boolean) {
    when (item.type) {
        MediaType.IMAGE -> ImagePage(item)
        MediaType.VIDEO -> VideoPage(item, isActive)
    }
}

// Small shim so this file doesn't need the accompanist systemuicontroller dependency
@Composable
private fun Modifier.statusBarsPaddingCompat(): Modifier = this.then(Modifier.padding(top = 8.dp))
