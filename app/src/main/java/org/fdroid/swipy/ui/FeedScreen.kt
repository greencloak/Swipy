package org.fdroid.swipy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.fdroid.swipy.data.MediaItem
import org.fdroid.swipy.data.MediaType
import org.fdroid.swipy.data.SortOrder

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    items: List<MediaItem>,
    sortOrder: SortOrder,
    onSortChange: (SortOrder) -> Unit,
    onOpenFolderPicker: () -> Unit,
    onShuffleNow: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { items.size })

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
                val item = items[page]
                val isActive = pagerState.currentPage == page
                when (item.type) {
                    MediaType.IMAGE -> ImagePage(
                        item = item,
                        onOpenFolderPicker = onOpenFolderPicker,
                        onShuffleNow = onShuffleNow,
                        onSortChange = onSortChange
                    )
                    MediaType.VIDEO -> VideoPage(
                        item = item,
                        isActive = isActive,
                        onOpenFolderPicker = onOpenFolderPicker,
                        onShuffleNow = onShuffleNow,
                        onSortChange = onSortChange
                    )
                }
            }
        }
    }
}
