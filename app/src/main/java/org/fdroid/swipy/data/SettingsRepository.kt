package org.fdroid.swipy.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

enum class ThemeMode { SYSTEM, LIGHT, DARK }

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
 * Everything here is written to disk immediately, so Android backgrounding
 * or resizing the app (e.g. split screen) never resets it.
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

    // Start-midway and remember-position are independent now — any
    // combination is allowed. When both are on and a saved position exists,
    // that position wins; midway is the fallback for videos with no saved
    // position yet.
    var startMidwayEnabled by mutableStateOf(prefs.getBoolean(KEY_START_MIDWAY, false))
        private set

    var rememberPositionEnabled by mutableStateOf(prefs.getBoolean(KEY_REMEMBER_POSITION, false))
        private set

    var autoAdvanceEnabled by mutableStateOf(prefs.getBoolean(KEY_AUTO_ADVANCE, false))
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

    /** Seed for "Shuffle" sort order, kept stable until the user re-shuffles. */
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

    fun updateStartMidwayEnabled(value: Boolean) {
        startMidwayEnabled = value
        prefs.edit().putBoolean(KEY_START_MIDWAY, value).apply()
    }

    fun updateRememberPositionEnabled(value: Boolean) {
        rememberPositionEnabled = value
        prefs.edit().putBoolean(KEY_REMEMBER_POSITION, value).apply()
    }

    fun updateAutoAdvanceEnabled(value: Boolean) {
        autoAdvanceEnabled = value
        prefs.edit().putBoolean(KEY_AUTO_ADVANCE, value).apply()
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

    /** Saves the current settings as "my default" so they can be restored later. */
    fun saveCurrentAsDefault() {
        prefs.edit()
            .putBoolean(KEY_DEFAULT_LOOP, loopEnabled)
            .putBoolean(KEY_DEFAULT_BRIGHTNESS, forceMaxBrightness)
            .putString(KEY_DEFAULT_THEME_MODE, themeMode.name)
            .putLong(KEY_DEFAULT_ACCENT, accentColor)
            .putBoolean(KEY_DEFAULT_START_MIDWAY, startMidwayEnabled)
            .putBoolean(KEY_DEFAULT_REMEMBER_POSITION, rememberPositionEnabled)
            .putBoolean(KEY_DEFAULT_AUTO_ADVANCE, autoAdvanceEnabled)
            .putString(KEY_DEFAULT_SORT_ORDER, sortOrder.name)
            .putStringSet(KEY_DEFAULT_FOLDERS, selectedFolders)
            .putStringSet(KEY_DEFAULT_ORIENTATIONS, selectedOrientations.map { it.name }.toSet())
            .apply()
    }

    /** Restores whatever was last saved via [saveCurrentAsDefault]. */
    fun restoreDefault() {
        updateLoopEnabled(prefs.getBoolean(KEY_DEFAULT_LOOP, true))
        updateForceMaxBrightness(prefs.getBoolean(KEY_DEFAULT_BRIGHTNESS, false))
        updateThemeMode(
            ThemeMode.valueOf(prefs.getString(KEY_DEFAULT_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        )
        updateAccentColor(prefs.getLong(KEY_DEFAULT_ACCENT, ACCENT_COLORS[0]))
        updateStartMidwayEnabled(prefs.getBoolean(KEY_DEFAULT_START_MIDWAY, false))
        updateRememberPositionEnabled(prefs.getBoolean(KEY_DEFAULT_REMEMBER_POSITION, false))
        updateAutoAdvanceEnabled(prefs.getBoolean(KEY_DEFAULT_AUTO_ADVANCE, false))
        updateSortOrder(
            SortOrder.valueOf(prefs.getString(KEY_DEFAULT_SORT_ORDER, SortOrder.DATE_NEWEST.name) ?: SortOrder.DATE_NEWEST.name)
        )
        updateSelectedFolders(prefs.getStringSet(KEY_DEFAULT_FOLDERS, emptySet()) ?: emptySet())
        updateSelectedOrientations(
            (prefs.getStringSet(KEY_DEFAULT_ORIENTATIONS, emptySet()) ?: emptySet())
                .mapNotNull { runCatching { Orientation.valueOf(it) }.getOrNull() }
                .toSet()
        )
    }

    /** Serializes the current settings (not liked media — that's exported separately). */
    fun exportSettingsJson(): String {
        val obj = JSONObject()
        obj.put("loopEnabled", loopEnabled)
        obj.put("forceMaxBrightness", forceMaxBrightness)
        obj.put("themeMode", themeMode.name)
        obj.put("accentColor", accentColor)
        obj.put("startMidwayEnabled", startMidwayEnabled)
        obj.put("rememberPositionEnabled", rememberPositionEnabled)
        obj.put("autoAdvanceEnabled", autoAdvanceEnabled)
        obj.put("sortOrder", sortOrder.name)
        obj.put("selectedFolders", JSONArray(selectedFolders.toList()))
        obj.put("selectedOrientations", JSONArray(selectedOrientations.map { it.name }))
        return obj.toString(2)
    }

    /** Applies settings previously written by [exportSettingsJson]. Returns false on malformed input. */
    fun importSettingsJson(json: String): Boolean {
        return try {
            val obj = JSONObject(json)
            updateLoopEnabled(obj.optBoolean("loopEnabled", true))
            updateForceMaxBrightness(obj.optBoolean("forceMaxBrightness", false))
            updateThemeMode(
                runCatching { ThemeMode.valueOf(obj.optString("themeMode", ThemeMode.SYSTEM.name)) }
                    .getOrDefault(ThemeMode.SYSTEM)
            )
            updateAccentColor(obj.optLong("accentColor", ACCENT_COLORS[0]))
            updateStartMidwayEnabled(obj.optBoolean("startMidwayEnabled", false))
            updateRememberPositionEnabled(obj.optBoolean("rememberPositionEnabled", false))
            updateAutoAdvanceEnabled(obj.optBoolean("autoAdvanceEnabled", false))
            updateSortOrder(
                runCatching { SortOrder.valueOf(obj.optString("sortOrder", SortOrder.DATE_NEWEST.name)) }
                    .getOrDefault(SortOrder.DATE_NEWEST)
            )
            val folders = obj.optJSONArray("selectedFolders")
            if (folders != null) {
                updateSelectedFolders((0 until folders.length()).map { folders.getString(it) }.toSet())
            }
            val orientations = obj.optJSONArray("selectedOrientations")
            if (orientations != null) {
                updateSelectedOrientations(
                    (0 until orientations.length())
                        .mapNotNull { runCatching { Orientation.valueOf(orientations.getString(it)) }.getOrNull() }
                        .toSet()
                )
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val KEY_LOOP = "loop_enabled"
        private const val KEY_BRIGHTNESS = "force_max_brightness"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ACCENT = "accent_color"
        private const val KEY_START_MIDWAY = "start_midway_enabled"
        private const val KEY_REMEMBER_POSITION = "remember_position_enabled"
        private const val KEY_AUTO_ADVANCE = "auto_advance_enabled"
        private const val KEY_FOLDERS = "selected_folders"
        private const val KEY_SORT_ORDER = "sort_order"
        private const val KEY_ORIENTATIONS = "selected_orientations"
        private const val KEY_LAST_VIEWED = "last_viewed_media_id"
        private const val KEY_SHUFFLE_SEED = "shuffle_seed"

        private const val KEY_DEFAULT_LOOP = "default_loop_enabled"
        private const val KEY_DEFAULT_BRIGHTNESS = "default_force_max_brightness"
        private const val KEY_DEFAULT_THEME_MODE = "default_theme_mode"
        private const val KEY_DEFAULT_ACCENT = "default_accent_color"
        private const val KEY_DEFAULT_START_MIDWAY = "default_start_midway_enabled"
        private const val KEY_DEFAULT_REMEMBER_POSITION = "default_remember_position_enabled"
        private const val KEY_DEFAULT_AUTO_ADVANCE = "default_auto_advance_enabled"
        private const val KEY_DEFAULT_SORT_ORDER = "default_sort_order"
        private const val KEY_DEFAULT_FOLDERS = "default_selected_folders"
        private const val KEY_DEFAULT_ORIENTATIONS = "default_selected_orientations"
    }
}
