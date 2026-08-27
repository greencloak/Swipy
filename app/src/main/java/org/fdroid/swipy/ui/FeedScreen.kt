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

/**
 * VerticalPager needs a bounded page count, so true infinite scrolling isn't
 * directly supported. The standard workaround: use a huge virtual page count
 * and map each virtual page to a real item via modulo. With Int.MAX_VALUE
 * pages and the start centered in the middle, you'd need to swipe billions
 * of times to hit either edge — effectively infinite for any real session.
 */
private const val VIRTUAL_PAGE_COUNT = Int.MAX_VALUE

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
    val itemCount = items.size

    val startVirtualPage = remember(items) {
        if (itemCount == 0) {
            0
        } else {
            val startIndex = items.indexOfFirst { it.id == initialItemId }.let { if (it >= 0) it else 0 }
            val middle = VIRTUAL_PAGE_COUNT / 2
            middle - (middle % itemCount) + startIndex
        }
    }
    val pagerState = rememberPagerState(
        initialPage = startVirtualPage,
        pageCount = { if (itemCount == 0) 0 else VIRTUAL_PAGE_COUNT }
    )
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage, items) {
        if (itemCount > 0) {
            onCurrentItemChanged(items[pagerState.currentPage.mod(itemCount)].id)
        }
    }

    // One-shot "jump here right now" signal from the in-feed shuffle button —
    // jump to the nearest virtual page (relative to where we currently are)
    // whose modulo lands on the target item.
    LaunchedEffect(jumpToItemId, items) {
        if (jumpToItemId != null && itemCount > 0) {
            val index = items.indexOfFirst { it.id == jumpToItemId }
            if (index >= 0) {
                val base = pagerState.currentPage - (pagerState.currentPage.mod(itemCount))
                pagerState.scrollToPage(base + index)
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
            ) { virtualPage ->
                val realIndex = virtualPage.mod(itemCount)
                val item = items[realIndex]
                val isActive = pagerState.currentPage == virtualPage
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
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(virtualPage + 1)
                            }
                        },
                        onOpenSettings = onOpenSettings
                    )
                }
            }
        }
    }
}
