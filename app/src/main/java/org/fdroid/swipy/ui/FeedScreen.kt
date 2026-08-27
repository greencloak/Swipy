package org.fdroid.swipy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
    onOpenSettings: () -> Unit
) {
    val startPage = remember(items) {
        items.indexOfFirst { it.id == initialItemId }.let { if (it >= 0) it else 0 }
    }
    val pagerState = rememberPagerState(initialPage = startPage, pageCount = { items.size })
    val coroutineScope = rememberCoroutineScope()

    // Persist whichever video is currently on screen, so relaunching the app
    // (or Android backgrounding it mid-swipe, e.g. for split screen) returns
    // to the same spot instead of resetting to the top.
    LaunchedEffect(pagerState.currentPage, items) {
        if (items.isNotEmpty()) {
            onCurrentItemChanged(items[pagerState.currentPage].id)
        }
    }

    // A one-shot "jump here right now" signal, used by the in-feed shuffle
    // button — an explicit scroll rather than waiting for the pager to
    // notice initialItemId changed (which it wouldn't, since that only
    // applies on first composition).
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
                        onOpenSettings = onOpenSettings
                    )
                }
            }
        }
    }
}
