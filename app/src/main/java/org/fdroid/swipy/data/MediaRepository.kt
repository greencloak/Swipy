package org.fdroid.swipy.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

/**
 * Reads local photos + videos from the device via MediaStore.
 * No network access, no analytics, no ads — everything stays on-device.
 */
class MediaRepository(private val context: Context) {

    /** Returns all distinct folder ("bucket") names found among images/videos. */
    fun listFolders(): List<String> {
        val folders = linkedSetOf<String>()
        folders.addAll(queryBuckets(MediaStore.Images.Media.EXTERNAL_CONTENT_URI))
        folders.addAll(queryBuckets(MediaStore.Video.Media.EXTERNAL_CONTENT_URI))
        return folders.sorted()
    }

    private fun queryBuckets(collection: android.net.Uri): Set<String> {
        val result = mutableSetOf<String>()
        val projection = arrayOf(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
        context.contentResolver.query(collection, projection, null, null, null)?.use { cursor ->
            val col = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                cursor.getString(col)?.let { result.add(it) }
            }
        }
        return result
    }

    /**
     * Loads media items, optionally filtered to [selectedFolders] (empty set = all folders)
     * and [selectedOrientations] (empty set = all shapes), sorted per [sortOrder].
     */
    fun loadMedia(
        selectedFolders: Set<String>,
        sortOrder: SortOrder,
        selectedOrientations: Set<Orientation> = emptySet(),
        includeImages: Boolean = true,
        includeVideos: Boolean = true
    ): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        if (includeImages) items.addAll(queryMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaType.IMAGE))
        if (includeVideos) items.addAll(queryMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaType.VIDEO))

        var filtered = if (selectedFolders.isEmpty()) {
            items
        } else {
            items.filter { it.bucketName in selectedFolders }
        }

        if (selectedOrientations.isNotEmpty()) {
            filtered = filtered.filter { it.orientation in selectedOrientations }
        }

        return when (sortOrder) {
            SortOrder.DATE_NEWEST -> filtered.sortedByDescending { it.dateAdded }
            SortOrder.DATE_OLDEST -> filtered.sortedBy { it.dateAdded }
            SortOrder.NAME_AZ -> filtered.sortedBy { it.displayName.lowercase() }
            SortOrder.RANDOM -> filtered.shuffled()
        }
    }

    private fun queryMedia(collection: android.net.Uri, type: MediaType): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT
        )
        context.contentResolver.query(collection, projection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                items.add(
                    MediaItem(
                        id = id,
                        uri = uri,
                        type = type,
                        displayName = cursor.getString(nameCol) ?: "",
                        bucketName = cursor.getString(bucketCol) ?: "Unknown",
                        dateAdded = cursor.getLong(dateCol),
                        width = cursor.getInt(widthCol),
                        height = cursor.getInt(heightCol)
                    )
                )
            }
        }
        return items
    }
}
