package com.yarnspace.app.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.yarnspace.app.data.settings.ThemeSettingsRepository

object ThemeModeCoordinator {

    fun applySavedNightMode(context: Context) {
        val settings = ThemeSettingsRepository(context).getThemeSettings()

        val mode = if (settings.useCustomTheme) {
            if (settings.forceNightMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        AppCompatDelegate.setDefaultNightMode(mode)
    }
}

