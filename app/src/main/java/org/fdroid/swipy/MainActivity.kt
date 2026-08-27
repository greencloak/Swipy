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
import org.fdroid.swipy.data.MediaRepository
import org.fdroid.swipy.data.PlaybackPositionStore
import org.fdroid.swipy.data.SettingsRepository
import org.fdroid.swipy.data.SortOrder
import org.fdroid.swipy.data.ThemeMode
import org.fdroid.swipy.ui.FeedScreen
import org.fdroid.swipy.ui.SettingsScreen

private enum class Screen { FEED, SETTINGS }

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
    // Only `screen` (which page you're on) and `refreshKey` (a one-off shuffle
    // trigger) stay as plain in-memory state — everything that should survive
    // the app being backgrounded (filters, sort, current video) now lives in
    // `settings`, which is backed by SharedPreferences.
    var screen by remember { mutableStateOf(Screen.FEED) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val positionStore = remember { PlaybackPositionStore(context) }

    val allFolders = remember { repository.listFolders() }
    val items = remember(settings.sortOrder, settings.selectedFolders, settings.selectedOrientations, refreshKey) {
        repository.loadMedia(settings.selectedFolders, settings.sortOrder, settings.selectedOrientations)
    }

    when (screen) {
        Screen.FEED -> FeedScreen(
            items = items,
            loopEnabled = settings.loopEnabled,
            playbackStartMode = settings.playbackStartMode,
            positionStore = positionStore,
            initialItemId = settings.lastViewedMediaId,
            onCurrentItemChanged = { settings.updateLastViewedMediaId(it) },
            onOpenSettings = { screen = Screen.SETTINGS }
        )
        Screen.SETTINGS -> SettingsScreen(
            settings = settings,
            allFolders = allFolders,
            onShuffleNow = {
                settings.updateSortOrder(SortOrder.RANDOM)
                refreshKey++
                screen = Screen.FEED
            },
            onBack = { screen = Screen.FEED }
        )
    }
}
