package org.fdroid.swipy.data

import android.net.Uri

enum class MediaType { IMAGE, VIDEO }

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val type: MediaType,
    val displayName: String,
    val bucketName: String,   // folder name, e.g. "Camera", "Downloads"
    val dateAdded: Long       // epoch seconds
)

enum class SortOrder(val label: String) {
    DATE_NEWEST("Newest first"),
    DATE_OLDEST("Oldest first"),
    NAME_AZ("Name (A-Z)"),
    RANDOM("Shuffle")
}
