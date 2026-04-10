package com.yarnspace.app.main

import android.app.Activity
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.widget.SwitchCompat
import androidx.core.widget.NestedScrollView
import com.yarnspace.app.R

data class SettingsPanelRefs(
    val settingsPanel: NestedScrollView,
    val settingsToggleButton: ImageButton,
    val customThemeSwitch: SwitchCompat,
    val darkModeSwitch: SwitchCompat,
    val darkModeRow: LinearLayout,
    val logoutButton: Button,
) {
    companion object {
        fun from(activity: Activity): SettingsPanelRefs {
            val darkModeSwitch = activity.findViewById<SwitchCompat>(R.id.switch_dark_mode)
            return SettingsPanelRefs(
                settingsPanel = activity.findViewById(R.id.settings_panel),
                settingsToggleButton = activity.findViewById(R.id.btn_settings_toggle),
                customThemeSwitch = activity.findViewById(R.id.switch_custom_theme),
                darkModeSwitch = darkModeSwitch,
                darkModeRow = darkModeSwitch.parent as LinearLayout,
                logoutButton = activity.findViewById(R.id.btn_logout),
            )
        }
    }
}

