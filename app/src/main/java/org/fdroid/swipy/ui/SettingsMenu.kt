package org.fdroid.swipy.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.fdroid.swipy.data.SortOrder

@Composable
fun SettingsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onOpenFolderPicker: () -> Unit,
    onShuffleNow: () -> Unit,
    onSortChange: (SortOrder) -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Select folders") },
            leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
            onClick = {
                onDismiss()
                onOpenFolderPicker()
            }
        )
        DropdownMenuItem(
            text = { Text("Shuffle now") },
            leadingIcon = { Icon(Icons.Default.Shuffle, contentDescription = null) },
            onClick = {
                onDismiss()
                onShuffleNow()
            }
        )
        HorizontalDivider()
        Text(
            "Sort by",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        SortOrder.entries.forEach { order ->
            DropdownMenuItem(
                text = { Text(order.label) },
                leadingIcon = { Icon(Icons.Default.Sort, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onSortChange(order)
                }
            )
        }
    }
}
