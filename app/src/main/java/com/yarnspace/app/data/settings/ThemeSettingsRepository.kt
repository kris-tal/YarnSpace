package com.yarnspace.app.data.settings

import android.content.Context

class ThemeSettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getThemeSettings(): ThemeSettings {
        return ThemeSettings(
            useCustomTheme = prefs.getBoolean(KEY_USE_CUSTOM_THEME, false),
            forceNightMode = prefs.getBoolean(KEY_FORCE_NIGHT_MODE, false),
        )
    }

    fun setUseCustomTheme(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_USE_CUSTOM_THEME, enabled).apply()
    }

    fun setForceDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FORCE_NIGHT_MODE, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_USE_CUSTOM_THEME = "use_custom_theme"
        private const val KEY_FORCE_NIGHT_MODE = "force_night_mode"
    }
}

