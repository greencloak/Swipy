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
import org.fdroid.swipy.data.Orientation
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
    var screen by remember { mutableStateOf(Screen.FEED) }
    var sortOrder by remember { mutableStateOf(SortOrder.DATE_NEWEST) }
    var selectedFolders by remember { mutableStateOf(setOf<String>()) }
    var selectedOrientations by remember { mutableStateOf(setOf<Orientation>()) }
    var refreshKey by remember { mutableIntStateOf(0) }

    val allFolders = remember { repository.listFolders() }
    val items = remember(sortOrder, selectedFolders, selectedOrientations, refreshKey) {
        repository.loadMedia(selectedFolders, sortOrder, selectedOrientations)
    }

    when (screen) {
        Screen.FEED -> FeedScreen(
            items = items,
            loopEnabled = settings.loopEnabled,
            onOpenSettings = { screen = Screen.SETTINGS }
        )
        Screen.SETTINGS -> SettingsScreen(
            settings = settings,
            sortOrder = sortOrder,
            onSortChange = { sortOrder = it },
            allFolders = allFolders,
            selectedFolders = selectedFolders,
            onFoldersChange = { selectedFolders = it },
            selectedOrientations = selectedOrientations,
            onOrientationsChange = { selectedOrientations = it },
            onShuffleNow = {
                sortOrder = SortOrder.RANDOM
                refreshKey++
                screen = Screen.FEED
            },
            onBack = { screen = Screen.FEED }
        )
    }
}
