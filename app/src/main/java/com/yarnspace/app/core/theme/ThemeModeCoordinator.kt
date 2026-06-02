package com.yarnspace.app.core.theme

import androidx.appcompat.app.AppCompatDelegate
import com.yarnspace.app.core.theme.settings.ThemeSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeModeCoordinator @Inject constructor(
    private val repository: ThemeSettingsRepository
) {
    fun applySavedNightMode() {
        val settings = repository.getThemeSettings()

        val mode = if (settings.useCustomTheme) {
            if (settings.forceNightMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
