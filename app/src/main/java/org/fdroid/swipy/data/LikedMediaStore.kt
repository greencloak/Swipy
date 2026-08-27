package org.fdroid.swipy.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Persists which media items the user has liked (hearted). */
class LikedMediaStore(context: Context) {
    private val prefs = context.getSharedPreferences("swipy_liked", Context.MODE_PRIVATE)

    var likedIds by mutableStateOf(
        (prefs.getStringSet(KEY_LIKED, emptySet()) ?: emptySet())
            .mapNotNull { it.toLongOrNull() }
            .toSet()
    )
        private set

    fun isLiked(id: Long): Boolean = id in likedIds

    fun toggle(id: Long) {
        likedIds = if (id in likedIds) likedIds - id else likedIds + id
        prefs.edit().putStringSet(KEY_LIKED, likedIds.map { it.toString() }.toSet()).apply()
    }

    /** Serializes liked media as JSON, given the matching display info for readability. */
    fun exportJson(nameLookup: (Long) -> String?): String {
        val array = org.json.JSONArray()
        likedIds.forEach { id ->
            val obj = org.json.JSONObject()
            obj.put("id", id)
            nameLookup(id)?.let { obj.put("name", it) }
            array.put(obj)
        }
        return array.toString(2)
    }

    companion object {
        private const val KEY_LIKED = "liked_ids"
    }
}
