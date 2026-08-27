package org.fdroid.swipy.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * How each video should start playback when first swiped onto.
 * Only one of these can be active — never both at once.
 */
enum class PlaybackStartMode { DEFAULT, START_MIDWAY, REMEMBER_POSITION }

/** A small curated palette the user can pick an accent color from. */
val ACCENT_COLORS = listOf(
    0xFF6750A4L, // purple (default)
    0xFFEF5350L, // red
    0xFFFF9800L, // orange
    0xFFFFC107L, // amber
    0xFF4CAF50L, // green
    0xFF009688L, // teal
    0xFF2196F3L, // blue
    0xFF3F51B5L, // indigo
    0xFFE91E63L  // pink
)

/**
 * Wraps SharedPreferences and exposes each setting as Compose state, so any
 * screen reading these properties recomposes automatically when they change.
 *
 * Everything here — including folder/sort/shape filters and the current
 * scroll position — is written to disk immediately, so Android backgrounding
 * or resizing the app (e.g. split screen) never resets it. Previously the
 * feed's filters and position lived only in in-memory Compose state, which
 * Android is free to wipe whenever the app leaves the foreground.
 */
class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("swipy_settings", Context.MODE_PRIVATE)

    var loopEnabled by mutableStateOf(prefs.getBoolean(KEY_LOOP, true))
        private set

    var forceMaxBrightness by mutableStateOf(prefs.getBoolean(KEY_BRIGHTNESS, false))
        private set

    var themeMode by mutableStateOf(
        ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
    )
        private set

    var accentColor by mutableStateOf(prefs.getLong(KEY_ACCENT, ACCENT_COLORS[0]))
        private set

    var playbackStartMode by mutableStateOf(
        PlaybackStartMode.valueOf(
            prefs.getString(KEY_PLAYBACK_START_MODE, PlaybackStartMode.DEFAULT.name) ?: PlaybackStartMode.DEFAULT.name
        )
    )
        private set

    var selectedFolders by mutableStateOf(
        (prefs.getStringSet(KEY_FOLDERS, emptySet()) ?: emptySet()).toSet()
    )
        private set

    var sortOrder by mutableStateOf(
        SortOrder.valueOf(prefs.getString(KEY_SORT_ORDER, SortOrder.DATE_NEWEST.name) ?: SortOrder.DATE_NEWEST.name)
    )
        private set

    var selectedOrientations by mutableStateOf(
        (prefs.getStringSet(KEY_ORIENTATIONS, emptySet()) ?: emptySet())
            .mapNotNull { runCatching { Orientation.valueOf(it) }.getOrNull() }
            .toSet()
    )
        private set

    /** The media id last visible in the feed, so we can jump back to it on relaunch. */
    var lastViewedMediaId by mutableStateOf(prefs.getLong(KEY_LAST_VIEWED, -1L))
        private set

    /**
     * Seed for "Shuffle" sort order. Without this, every time Android
     * recreates the activity (e.g. resizing a Samsung split-screen pane)
     * the RANDOM order would reshuffle from scratch — looking exactly like
     * the whole library reloaded, even though nothing else changed. Reusing
     * the same seed keeps the order stable until the user explicitly taps
     * "Shuffle now" again, which generates a fresh seed.
     */
    var shuffleSeed by mutableStateOf(prefs.getLong(KEY_SHUFFLE_SEED, System.currentTimeMillis()))
        private set

    fun updateLoopEnabled(value: Boolean) {
        loopEnabled = value
        prefs.edit().putBoolean(KEY_LOOP, value).apply()
    }

    fun updateForceMaxBrightness(value: Boolean) {
        forceMaxBrightness = value
        prefs.edit().putBoolean(KEY_BRIGHTNESS, value).apply()
    }

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun updateAccentColor(color: Long) {
        accentColor = color
        prefs.edit().putLong(KEY_ACCENT, color).apply()
    }

    fun updatePlaybackStartMode(mode: PlaybackStartMode) {
        playbackStartMode = mode
        prefs.edit().putString(KEY_PLAYBACK_START_MODE, mode.name).apply()
    }

    fun updateSelectedFolders(folders: Set<String>) {
        selectedFolders = folders
        prefs.edit().putStringSet(KEY_FOLDERS, folders).apply()
    }

    fun updateSortOrder(order: SortOrder) {
        sortOrder = order
        prefs.edit().putString(KEY_SORT_ORDER, order.name).apply()
    }

    fun updateSelectedOrientations(orientations: Set<Orientation>) {
        selectedOrientations = orientations
        prefs.edit().putStringSet(KEY_ORIENTATIONS, orientations.map { it.name }.toSet()).apply()
    }

    fun updateLastViewedMediaId(id: Long) {
        lastViewedMediaId = id
        prefs.edit().putLong(KEY_LAST_VIEWED, id).apply()
    }

    fun updateShuffleSeed(seed: Long) {
        shuffleSeed = seed
        prefs.edit().putLong(KEY_SHUFFLE_SEED, seed).apply()
    }

    companion object {
        private const val KEY_LOOP = "loop_enabled"
        private const val KEY_BRIGHTNESS = "force_max_brightness"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ACCENT = "accent_color"
        private const val KEY_PLAYBACK_START_MODE = "playback_start_mode"
        private const val KEY_FOLDERS = "selected_folders"
        private const val KEY_SORT_ORDER = "sort_order"
        private const val KEY_ORIENTATIONS = "selected_orientations"
        private const val KEY_LAST_VIEWED = "last_viewed_media_id"
        private const val KEY_SHUFFLE_SEED = "shuffle_seed"
    }
}
