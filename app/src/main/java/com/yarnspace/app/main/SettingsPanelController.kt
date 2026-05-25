package com.yarnspace.app.main

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.yarnspace.app.core.auth.TokenManager
import com.yarnspace.app.core.auth.SessionRepository
import com.yarnspace.app.core.network.ApiService
import com.yarnspace.app.data.settings.ThemeSettingsRepository
import kotlinx.coroutines.launch

class SettingsPanelController(
    private val context: Context,
    private val refs: SettingsPanelRefs,
    private val themeSettingsRepository: ThemeSettingsRepository,
    private val sessionRepository: SessionRepository,
    private val apiService: ApiService,
    private val resources: Resources,
    private val onLogout: () -> Unit,
) {
    fun bind(isInitiallyOpen: Boolean) {
        val themeSettings = themeSettingsRepository.getThemeSettings()
        val useCustomTheme = themeSettings.useCustomTheme
        val forceNight = themeSettings.forceNightMode

        refs.customThemeSwitch.isChecked = useCustomTheme
        refs.darkModeSwitch.isChecked = if (useCustomTheme) forceNight else isSystemCurrentlyDark()
        setDarkModeEnabled(useCustomTheme)
        setPanelVisible(isInitiallyOpen)

        refs.settingsToggleButton.setOnClickListener {
            setPanelVisible(!isPanelOpen())
        }

        refs.customThemeSwitch.setOnCheckedChangeListener { _, isChecked ->
            setDarkModeEnabled(isChecked)
            themeSettingsRepository.setUseCustomTheme(isChecked)

            if (!isChecked) {
                refs.darkModeSwitch.isChecked = isSystemCurrentlyDark()
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            } else {
                applyCustomMode(refs.darkModeSwitch.isChecked)
            }
        }

        refs.darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (refs.customThemeSwitch.isChecked) {
                themeSettingsRepository.setForceDarkMode(isChecked)
                applyCustomMode(isChecked)
            }
        }

        refs.logoutButton.setOnClickListener {
            (context as? AppCompatActivity)?.lifecycleScope?.launch {
                try {
                    apiService.logout()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    TokenManager.clearToken(context)
                    sessionRepository.clearSession()
                    themeSettingsRepository.clearAccentColor()
                    onLogout()
                }
            } ?: run {
                TokenManager.clearToken(context)
                sessionRepository.clearSession()
                themeSettingsRepository.clearAccentColor()
                onLogout()
            }
        }
    }

    fun onResume() {
        if (!refs.customThemeSwitch.isChecked) {
            refs.darkModeSwitch.isChecked = isSystemCurrentlyDark()
        }
    }

    fun closePanelIfOpen() {
        if (isPanelOpen()) {
            setPanelVisible(false)
        }
    }

    fun isPanelOpen(): Boolean = refs.settingsPanel.visibility == View.VISIBLE

    private fun setDarkModeEnabled(enabled: Boolean) {
        refs.darkModeSwitch.isEnabled = enabled
        refs.darkModeRow.alpha = if (enabled) 1f else 0.45f
    }

    private fun setPanelVisible(visible: Boolean) {
        refs.settingsPanel.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun applyCustomMode(darkEnabled: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (darkEnabled) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO,
        )
    }

    private fun isSystemCurrentlyDark(): Boolean {
        return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }
}
