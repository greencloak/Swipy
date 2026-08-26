package org.fdroid.swipy.data

import android.net.Uri

enum class MediaType { IMAGE, VIDEO }

enum class Orientation(val label: String) {
    SQUARE("Square"),
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape")
}

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val type: MediaType,
    val displayName: String,
    val bucketName: String,   // folder name, e.g. "Camera", "Downloads"
    val dateAdded: Long,      // epoch seconds
    val width: Int = 0,
    val height: Int = 0
) {
    val orientation: Orientation
        get() {
            if (width <= 0 || height <= 0) return Orientation.LANDSCAPE
            val ratio = width.toFloat() / height.toFloat()
            return when {
                ratio in 0.95f..1.05f -> Orientation.SQUARE
                height > width -> Orientation.PORTRAIT
                else -> Orientation.LANDSCAPE
            }
        }
}

enum class SortOrder(val label: String) {
    DATE_NEWEST("Newest first"),
    DATE_OLDEST("Oldest first"),
    NAME_AZ("Name (A-Z)"),
    RANDOM("Shuffle")
}
