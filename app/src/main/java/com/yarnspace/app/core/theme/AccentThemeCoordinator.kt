package com.yarnspace.app.core.theme

import android.content.Context
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import com.yarnspace.app.core.theme.settings.ThemeSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccentThemeCoordinator @Inject constructor(
    private val repository: ThemeSettingsRepository
) {
    fun applySavedAccent(activity: AppCompatActivity) {
        val name = repository.getAccentColorName()
        applyAccent(activity, name)
    }

    fun applyAccent(activity: AppCompatActivity, accentBackendName: String?) {
        val overlay = AccentColor.fromBackendName(accentBackendName).themeOverlayResId
        applyOverlay(activity, overlay)
    }

    private fun applyOverlay(activity: AppCompatActivity, @StyleRes overlayResId: Int) {
        activity.theme.applyStyle(overlayResId, true)
    }

    fun resolveOverlayResId(context: Context, accentBackendName: String?): Int {
        return AccentColor.fromBackendName(accentBackendName).themeOverlayResId
    }
}
