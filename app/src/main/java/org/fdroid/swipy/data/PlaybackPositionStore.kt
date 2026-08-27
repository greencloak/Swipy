package org.fdroid.swipy.data

import android.content.Context

/** Persists each video's last playback position so it can resume where you left off. */
class PlaybackPositionStore(context: Context) {
    private val prefs = context.getSharedPreferences("swipy_positions", Context.MODE_PRIVATE)

    fun getPosition(mediaId: Long): Long = prefs.getLong(mediaId.toString(), 0L)

    fun savePosition(mediaId: Long, positionMs: Long) {
        prefs.edit().putLong(mediaId.toString(), positionMs).apply()
    }

    /** Wipes every remembered position across the whole library. */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
