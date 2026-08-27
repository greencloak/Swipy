package org.fdroid.swipy

import android.Manifest
import android.os.Build
import android.os.Bundle
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
import androidx.compose.ui.unit.dp
import org.fdroid.swipy.data.LikedMediaStore
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

            // Force-maximum-brightness setting applies at the window level.
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

    // One-shot signals for the in-feed "shuffle and jump to a random point"
    // button: which item to scroll to right now, and which item (once it
    // loads) should start playback from a random position instead of the
    // usual midway/remember-position logic.
    var jumpToItemId by remember { mutableStateOf<Long?>(null) }
    var randomStartItemId by remember { mutableStateOf<Long?>(null) }

    val allFolders = remember { repository.listFolders() }
    val items = remember(
        settings.sortOrder,
        settings.selectedFolders,
        settings.selectedOrientations,
        settings.shuffleSeed
    ) {
        repository.loadMedia(
            selectedFolders = settings.selectedFolders,
            sortOrder = settings.sortOrder,
            selectedOrientations = settings.selectedOrientations,
            shuffleSeed = settings.shuffleSeed
        )
    }

    val onShuffleAndRandomStart: () -> Unit = {
        // Computed synchronously (not via the `items` remember above, which
        // only updates after recomposition) so we can pick a random target
        // from the *new* order immediately.
        val newSeed = System.currentTimeMillis()
        val newItems = repository.loadMedia(
            selectedFolders = settings.selectedFolders,
            sortOrder = SortOrder.RANDOM,
            selectedOrientations = settings.selectedOrientations,
            shuffleSeed = newSeed
        )
        settings.updateShuffleSeed(newSeed)
        settings.updateSortOrder(SortOrder.RANDOM)
        val target = newItems.randomOrNull()
        if (target != null) {
            jumpToItemId = target.id
            randomStartItemId = target.id
            settings.updateLastViewedMediaId(target.id)
        }
    }

    when (screen) {
        Screen.FEED -> FeedScreen(
            items = items,
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
            onOpenSettings = { screen = Screen.SETTINGS }
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
            onOpenLikedGallery = { screen = Screen.LIKED_GALLERY },
            onBack = { screen = Screen.FEED }
        )
        Screen.LIKED_GALLERY -> {
            val allItems = remember {
                repository.loadMedia(emptySet(), SortOrder.DATE_NEWEST, emptySet())
            }
            val likedItems = allItems.filter { likedStore.isLiked(it.id) }
            LikedGalleryScreen(
                likedItems = likedItems,
                onItemSelected = { item ->
                    settings.updateLastViewedMediaId(item.id)
                    screen = Screen.FEED
                },
                onBack = { screen = Screen.SETTINGS }
            )
        }
    }
}
