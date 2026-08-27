package org.fdroid.swipy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.fdroid.swipy.data.ACCENT_COLORS
import org.fdroid.swipy.data.LikedMediaStore
import org.fdroid.swipy.data.Orientation
import org.fdroid.swipy.data.PlaybackPositionStore
import org.fdroid.swipy.data.PlaybackStartMode
import org.fdroid.swipy.data.SettingsRepository
import org.fdroid.swipy.data.SortOrder
import org.fdroid.swipy.data.ThemeMode

private data class SettingRow(val label: String, val content: @Composable () -> Unit)
private data class SettingSection(val title: String, val rows: List<SettingRow>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsRepository,
    positionStore: PlaybackPositionStore,
    likedStore: LikedMediaStore,
    allFolders: List<String>,
    onShuffleNow: () -> Unit,
    onOpenLikedGallery: () -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showResetPositionsConfirm by remember { mutableStateOf(false) }

    val sections = listOf(
        SettingSection(
            title = "Playback",
            rows = listOf(
                SettingRow("Loop videos") {
                    SettingSwitchRow(
                        label = "Loop videos",
                        description = "Restart automatically when a video ends",
                        checked = settings.loopEnabled,
                        onCheckedChange = { settings.updateLoopEnabled(it) }
                    )
                },
                SettingRow("Playback start") { PlaybackStartRow(settings) },
                SettingRow("Reset remembered positions") {
                    ClickableSettingRow(
                        label = "Reset remembered positions",
                        description = "Clear every saved resume point across the whole library",
                        onClick = { showResetPositionsConfirm = true }
                    )
                }
            )
        ),
        SettingSection(
            title = "Display",
            rows = listOf(
                SettingRow("Force maximum brightness") {
                    SettingSwitchRow(
                        label = "Force maximum brightness",
                        description = "Keep the screen at full brightness while Swipy is open",
                        checked = settings.forceMaxBrightness,
                        onCheckedChange = { settings.updateForceMaxBrightness(it) }
                    )
                }
            )
        ),
        SettingSection(
            title = "Appearance",
            rows = listOf(
                SettingRow("Theme") { ThemeModeRow(settings) },
                SettingRow("Accent color") { AccentColorRow(settings) }
            )
        ),
        SettingSection(
            title = "Liked",
            rows = listOf(
                SettingRow("View liked media") {
                    ClickableSettingRow(
                        label = "View liked media",
                        description = "${likedStore.likedIds.size} liked — browse them in a gallery",
                        icon = Icons.Default.Favorite,
                        onClick = onOpenLikedGallery
                    )
                }
            )
        ),
        SettingSection(
            title = "Library",
            rows = listOf(
                SettingRow("Select folders") {
                    ClickableSettingRow(
                        label = "Select folders",
                        description = if (settings.selectedFolders.isEmpty()) "All folders" else "${settings.selectedFolders.size} selected",
                        onClick = { showFolderPicker = true }
                    )
                },
                SettingRow("Sort order") { SortOrderRow(settings.sortOrder) { settings.updateSortOrder(it) } },
                SettingRow("Filter by shape") {
                    OrientationFilterRow(settings.selectedOrientations) { settings.updateSelectedOrientations(it) }
                },
                SettingRow("Shuffle now") {
                    ClickableSettingRow(
                        label = "Shuffle now",
                        description = "Re-randomize the feed immediately",
                        icon = Icons.Default.Shuffle,
                        onClick = onShuffleNow
                    )
                }
            )
        )
    )

    val filteredSections = if (query.isBlank()) {
        sections
    } else {
        sections
            .map { section -> section.copy(rows = section.rows.filter { it.label.contains(query, ignoreCase = true) }) }
            .filter { it.rows.isNotEmpty() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search settings") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            filteredSections.forEach { section ->
                item {
                    Text(
                        section.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
                    )
                }
                items(section.rows) { row -> row.content() }
            }
        }
    }

    if (showFolderPicker) {
        FolderPickerScreen(
            allFolders = allFolders,
            selectedFolders = settings.selectedFolders,
            onConfirm = {
                settings.updateSelectedFolders(it)
                showFolderPicker = false
            },
            onDismiss = { showFolderPicker = false }
        )
    }

    if (showResetPositionsConfirm) {
        AlertDialog(
            onDismissRequest = { showResetPositionsConfirm = false },
            title = { Text("Reset remembered positions?") },
            text = { Text("This clears every saved resume point across your whole library. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    positionStore.clearAll()
                    showResetPositionsConfirm = false
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetPositionsConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ClickableSettingRow(
    label: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
        }
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ThemeModeRow(settings: SettingsRepository) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Theme", style = MaterialTheme.typography.bodyLarge)
        Row(Modifier.padding(top = 8.dp)) {
            ThemeMode.entries.forEach { mode ->
                val selected = settings.themeMode == mode
                Text(
                    text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable { settings.updateThemeMode(mode) }
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun AccentColorRow(settings: SettingsRepository) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Accent color", style = MaterialTheme.typography.bodyLarge)
        Row(Modifier.padding(top = 8.dp)) {
            ACCENT_COLORS.forEach { colorLong ->
                val selected = settings.accentColor == colorLong
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(36.dp)
                        .background(Color(colorLong), CircleShape)
                        .border(
                            width = if (selected) 3.dp else 0.dp,
                            color = MaterialTheme.colorScheme.onBackground,
                            shape = CircleShape
                        )
                        .clickable { settings.updateAccentColor(colorLong) }
                )
            }
        }
    }
}

@Composable
private fun SortOrderRow(sortOrder: SortOrder, onSortChange: (SortOrder) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Sort order", style = MaterialTheme.typography.bodyLarge)
        SortOrder.entries.forEach { order ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSortChange(order) }
                    .padding(vertical = 6.dp)
            ) {
                RadioButton(selected = sortOrder == order, onClick = { onSortChange(order) })
                Text(order.label)
            }
        }
    }
}

@Composable
private fun OrientationFilterRow(
    selected: Set<Orientation>,
    onChange: (Set<Orientation>) -> Unit
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Filter by shape", style = MaterialTheme.typography.bodyLarge)
        Text(
            "Select any combination. Leave none checked to show everything.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(Modifier.padding(top = 8.dp)) {
            Orientation.entries.forEach { orientation ->
                val isSelected = orientation in selected
                Text(
                    text = orientation.label,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable {
                            onChange(
                                if (isSelected) selected - orientation else selected + orientation
                            )
                        }
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PlaybackStartRow(settings: SettingsRepository) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Playback start", style = MaterialTheme.typography.bodyLarge)
        Text(
            "Only one of these can be on at a time.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SettingSwitchRow(
            label = "Start midway",
            description = "Begin each video partway through the first time you swipe onto it",
            checked = settings.playbackStartMode == PlaybackStartMode.START_MIDWAY,
            onCheckedChange = {
                settings.updatePlaybackStartMode(
                    if (it) PlaybackStartMode.START_MIDWAY else PlaybackStartMode.DEFAULT
                )
            }
        )
        SettingSwitchRow(
            label = "Remember last position",
            description = "Resume each video where you left off last time",
            checked = settings.playbackStartMode == PlaybackStartMode.REMEMBER_POSITION,
            onCheckedChange = {
                settings.updatePlaybackStartMode(
                    if (it) PlaybackStartMode.REMEMBER_POSITION else PlaybackStartMode.DEFAULT
                )
            }
        )
    }
}
