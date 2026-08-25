package org.fdroid.swipy

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.fdroid.swipy.data.MediaRepository
import org.fdroid.swipy.data.SortOrder
import org.fdroid.swipy.ui.FeedScreen
import org.fdroid.swipy.ui.FolderPickerScreen

class MainActivity : ComponentActivity() {

    private val repository by lazy { MediaRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var hasPermission by remember { mutableStateOf(hasMediaPermission()) }

            val permissionLauncher = rememberLauncherForPermissions { granted ->
                hasPermission = granted
            }

            MaterialTheme(colorScheme = darkColorScheme()) {
                if (hasPermission) {
                    SwipyApp(repository = repository)
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
private fun SwipyApp(repository: MediaRepository) {
    var sortOrder by remember { mutableStateOf(SortOrder.DATE_NEWEST) }
    var selectedFolders by remember { mutableStateOf(setOf<String>()) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }

    val allFolders = remember { repository.listFolders() }
    val items = remember(sortOrder, selectedFolders, refreshKey) {
        repository.loadMedia(selectedFolders, sortOrder)
    }

    FeedScreen(
        items = items,
        sortOrder = sortOrder,
        onSortChange = { sortOrder = it },
        onOpenFolderPicker = { showFolderPicker = true },
        onShuffleNow = {
            sortOrder = SortOrder.RANDOM
            refreshKey++ // forces a fresh shuffle even if already RANDOM
        }
    )

    if (showFolderPicker) {
        FolderPickerScreen(
            allFolders = allFolders,
            selectedFolders = selectedFolders,
            onConfirm = {
                selectedFolders = it
                showFolderPicker = false
            },
            onDismiss = { showFolderPicker = false }
        )
    }
}
