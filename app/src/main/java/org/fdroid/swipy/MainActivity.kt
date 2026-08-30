package org.fdroid.swipy

import android.Manifest
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fdroid.swipy.data.LikedMediaStore
import org.fdroid.swipy.data.MediaItem
import org.fdroid.swipy.data.MediaRepository
import org.fdroid.swipy.data.PlaybackPositionStore
import org.fdroid.swipy.data.SettingsRepository
import org.fdroid.swipy.data.SortOrder
import org.fdroid.swipy.data.ThemeMode
import org.fdroid.swipy.ui.FeedScreen
import org.fdroid.swipy.ui.LikedGalleryScreen
import org.fdroid.swipy.ui.SettingsScreen

private enum class Screen { FEED, SETTINGS, LIKED_GALLERY }

class MainActivity : ComponentActivity() {

    private val repository by lazy { MediaRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var hasPermission by remember { mutableStateOf(hasMediaPermission()) }

            val permissionLauncher = rememberLauncherForPermissions { granted ->
                hasPermission = granted
            }

            val settings = remember { SettingsRepository(applicationContext) }

            LaunchedEffect(settings.forceMaxBrightness) {
                val attrs = window.attributes
                attrs.screenBrightness = if (settings.forceMaxBrightness) {
                    WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                } else {
                    WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
                window.attributes = attrs
            }

            val isDark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val accent = Color(settings.accentColor)
            val colorScheme = if (isDark) {
                darkColorScheme(primary = accent, secondary = accent)
            } else {
                lightColorScheme(primary = accent, secondary = accent)
            }

            MaterialTheme(colorScheme = colorScheme) {
                if (hasPermission) {
                    SwipyApp(repository = repository, settings = settings)
                } else {
                    PermissionRequestScreen(onRequest = { permissionLauncher() })
                }
            }
        }
    }

    private fun hasMediaPermission(): Boolean {
        val perms = requiredPermissions()
        return perms.all {
            checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    @Composable
    private fun rememberLauncherForPermissions(onResult: (Boolean) -> Unit): () -> Unit {
        val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            onResult(result.values.all { it })
        }
        val perms = requiredPermissions()
        return { launcher.launch(perms) }
    }
}

@Composable
private fun PermissionRequestScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Swipy needs access to your photos and videos to build your feed.")
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequest) { Text("Grant access") }
    }
}

@Composable
private fun SwipyApp(repository: MediaRepository, settings: SettingsRepository) {
    var screen by remember { mutableStateOf(Screen.FEED) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val positionStore = remember { PlaybackPositionStore(context) }
    val likedStore = remember { LikedMediaStore(context) }
    val coroutineScope = rememberCoroutineScope()

    // When true, the feed shows only liked items (entered by tapping
    // something in the Liked gallery) instead of the normal library.
    var likedFeedActive by remember { mutableStateOf(false) }

    var jumpToItemId by remember { mutableStateOf<Long?>(null) }
    var randomStartItemId by remember { mutableStateOf<Long?>(null) }

    val allFolders = remember { repository.listFolders() }

    // Bumped whenever MediaStore reports a change (via the ContentObserver
    // below, debounced) or the user taps a manual refresh action. Used as a
    // LaunchedEffect key so both `items` and `allItemsUnfiltered` re-query
    // MediaStore without requiring an app restart.
    var mediaChangeTick by remember { mutableStateOf(0L) }

    // Registers a single ContentObserver spanning both the Images and Video
    // collections while the app is in the foreground (ON_START/ON_STOP) —
    // Swipy does no background work, so there's nothing to gain from eating
    // change notifications the user can't see. Bursts of onChange callbacks
    // (e.g. a sync app writing many files back-to-back) are debounced by
    // 500ms so a batch import triggers exactly one refresh, not one per file.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, repository) {
        var observer: ContentObserver? = null
        var debounceJob: Job? = null

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    observer = repository.registerChangeObserver(Handler(Looper.getMainLooper())) {
                        debounceJob?.cancel()
                        debounceJob = coroutineScope.launch {
                            delay(500)
                            mediaChangeTick = System.currentTimeMillis()
                        }
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    observer?.let { repository.unregisterChangeObserver(it) }
                    observer = null
                    debounceJob?.cancel()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            observer?.let { repository.unregisterChangeObserver(it) }
            debounceJob?.cancel()
        }
    }

    // Manual refresh: user-initiated, so it skips the debounce entirely and
    // triggers an immediate reload.
    val onManualRefresh: () -> Unit = { mediaChangeTick = System.currentTimeMillis() }

    // MediaStore queries now run off the main thread — with a ContentObserver
    // able to trigger a reload at any time, a synchronous query here would
    // risk a dropped frame right as the user is mid-swipe.
    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    LaunchedEffect(
        settings.sortOrder,
        settings.selectedFolders,
        settings.selectedOrientations,
        settings.shuffleSeed,
        mediaChangeTick
    ) {
        items = withContext(Dispatchers.IO) {
            repository.loadMedia(
                selectedFolders = settings.selectedFolders,
                sortOrder = settings.sortOrder,
                selectedOrientations = settings.selectedOrientations,
                shuffleSeed = settings.shuffleSeed
            )
        }
    }

    // Unfiltered library, used to resolve which MediaItems are liked —
    // shared between the gallery and the liked-only feed mode. Re-queried on
    // the same mediaChangeTick as `items` so newly-added media resolves here
    // too (e.g. liking something you just synced onto the device).
    var allItemsUnfiltered by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    LaunchedEffect(mediaChangeTick) {
        allItemsUnfiltered = withContext(Dispatchers.IO) {
            repository.loadMedia(emptySet(), SortOrder.DATE_NEWEST, emptySet())
        }
    }

    val likedItemsList = remember(likedStore.likedIds, allItemsUnfiltered) {
        allItemsUnfiltered.filter { likedStore.isLiked(it.id) }
    }

    val displayedItems = if (likedFeedActive) likedItemsList else items

    val onShuffleAndRandomStart: () -> Unit = {
        val newSeed = System.currentTimeMillis()
        coroutineScope.launch {
            val newItems = withContext(Dispatchers.IO) {
                repository.loadMedia(
                    selectedFolders = settings.selectedFolders,
                    sortOrder = SortOrder.RANDOM,
                    selectedOrientations = settings.selectedOrientations,
                    shuffleSeed = newSeed
                )
            }
            settings.updateShuffleSeed(newSeed)
            settings.updateSortOrder(SortOrder.RANDOM)
            val target = newItems.randomOrNull()
            if (target != null) {
                jumpToItemId = target.id
                randomStartItemId = target.id
                settings.updateLastViewedMediaId(target.id)
            }
        }
    }

    when (screen) {
        Screen.FEED -> FeedScreen(
            items = displayedItems,
            loopEnabled = settings.loopEnabled,
            startMidwayEnabled = settings.startMidwayEnabled,
            rememberPositionEnabled = settings.rememberPositionEnabled,
            autoAdvanceEnabled = settings.autoAdvanceEnabled,
            positionStore = positionStore,
            likedStore = likedStore,
            initialItemId = settings.lastViewedMediaId,
            onCurrentItemChanged = { settings.updateLastViewedMediaId(it) },
            jumpToItemId = jumpToItemId,
            onJumpHandled = { jumpToItemId = null },
            randomStartItemId = randomStartItemId,
            onRandomStartConsumed = { randomStartItemId = null },
            onShuffleAndRandomStart = onShuffleAndRandomStart,
            onRefresh = onManualRefresh,
            onOpenSettings = {
                // Leaving to Settings always returns to the full library
                // afterward, rather than staying pinned to liked-only.
                likedFeedActive = false
                screen = Screen.SETTINGS
            }
        )
        Screen.SETTINGS -> SettingsScreen(
            settings = settings,
            positionStore = positionStore,
            likedStore = likedStore,
            repository = repository,
            allFolders = allFolders,
            onShuffleNow = {
                settings.updateShuffleSeed(System.currentTimeMillis())
                settings.updateSortOrder(SortOrder.RANDOM)
                screen = Screen.FEED
            },
            onManualRefresh = onManualRefresh,
            onOpenLikedGallery = { screen = Screen.LIKED_GALLERY },
            onBack = { screen = Screen.FEED }
        )
        Screen.LIKED_GALLERY -> {
            LikedGalleryScreen(
                likedItems = likedItemsList,
                onItemSelected = { item ->
                    settings.updateLastViewedMediaId(item.id)
                    likedFeedActive = true
                    jumpToItemId = item.id
                    screen = Screen.FEED
                },
                onBack = { screen = Screen.SETTINGS }
            )
        }
    }
}
