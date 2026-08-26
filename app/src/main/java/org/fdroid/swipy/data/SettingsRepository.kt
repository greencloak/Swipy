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

    companion object {
        private const val KEY_LOOP = "loop_enabled"
        private const val KEY_BRIGHTNESS = "force_max_brightness"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ACCENT = "accent_color"
        private const val KEY_PLAYBACK_START_MODE = "playback_start_mode"
    }
}
