package org.fdroid.swipy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.fdroid.swipy.data.LikedMediaStore
import org.fdroid.swipy.data.MediaItem
import org.fdroid.swipy.data.MediaType
import org.fdroid.swipy.data.PlaybackPositionStore

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    items: List<MediaItem>,
    loopEnabled: Boolean,
    startMidwayEnabled: Boolean,
    rememberPositionEnabled: Boolean,
    autoAdvanceEnabled: Boolean,
    positionStore: PlaybackPositionStore,
    likedStore: LikedMediaStore,
    initialItemId: Long,
    onCurrentItemChanged: (Long) -> Unit,
    jumpToItemId: Long?,
    onJumpHandled: () -> Unit,
    randomStartItemId: Long?,
    onRandomStartConsumed: () -> Unit,
    onShuffleAndRandomStart: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val startPage = remember(items) {
        items.indexOfFirst { it.id == initialItemId }.let { if (it >= 0) it else 0 }
    }
    val pagerState = rememberPagerState(initialPage = startPage, pageCount = { items.size })
    val coroutineScope = rememberCoroutineScope()

    // Tracks whatever item is currently on screen, independent of its index —
    // used below to re-anchor the pager if a refresh reorders the list
    // (e.g. a newly-synced photo pushes everything after it down by one).
    var currentItemId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(pagerState.currentPage, items) {
        if (items.isNotEmpty()) {
            val item = items[pagerState.currentPage]
            currentItemId = item.id
            onCurrentItemChanged(item.id)
        }
    }

    // Self-correct position when the item list changes out from under us —
    // most notably a ContentObserver-triggered refresh — but never fight an
    // in-progress swipe: if the user is actively dragging, skip this pass.
    // `items` is already correct at that point, just possibly at a different
    // index than before; the next stable frame stays internally consistent.
    LaunchedEffect(items) {
        val targetId = currentItemId ?: return@LaunchedEffect
        if (pagerState.isScrollInProgress) return@LaunchedEffect
        val newIndex = items.indexOfFirst { it.id == targetId }
        if (newIndex >= 0 && newIndex != pagerState.currentPage) {
            pagerState.scrollToPage(newIndex)
        }
    }

    LaunchedEffect(jumpToItemId, items) {
        if (jumpToItemId != null) {
            val index = items.indexOfFirst { it.id == jumpToItemId }
            if (index >= 0) {
                pagerState.scrollToPage(index)
            }
            onJumpHandled()
        }
    }

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
                        likedStore = likedStore,
                        onShuffleAndRandomStart = onShuffleAndRandomStart,
                        onRefresh = onRefresh,
                        onOpenSettings = onOpenSettings
                    )
                    MediaType.VIDEO -> VideoPage(
                        item = item,
                        isActive = isActive,
                        loopEnabled = loopEnabled,
                        startMidwayEnabled = startMidwayEnabled,
                        rememberPositionEnabled = rememberPositionEnabled,
                        autoAdvanceEnabled = autoAdvanceEnabled,
                        positionStore = positionStore,
                        likedStore = likedStore,
                        randomStartItemId = randomStartItemId,
                        onRandomStartConsumed = onRandomStartConsumed,
                        onShuffleAndRandomStart = onShuffleAndRandomStart,
                        onVideoEnded = {
                            if (page < items.lastIndex) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(page + 1)
                                }
                            }
                        },
                        onRefresh = onRefresh,
                        onOpenSettings = onOpenSettings
                    )
                }
            }
        }
    }
}
