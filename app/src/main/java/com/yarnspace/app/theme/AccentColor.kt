package com.yarnspace.app.theme

import androidx.annotation.ColorRes
import androidx.annotation.StyleRes
import com.yarnspace.app.R

enum class AccentColor(
    val backendName: String,
    @param:ColorRes val colorResId: Int,
    @param:ColorRes val nightColorResId: Int,
    @param:StyleRes val themeOverlayResId: Int,
) {
    SAGE("sage", R.color.accent_sage, R.color.accent_sage_night, R.style.ThemeOverlay_YarnSpace_Accent_Sage),
    PEACH("peach", R.color.accent_peach, R.color.accent_peach_night, R.style.ThemeOverlay_YarnSpace_Accent_Peach),
    LAVENDER("lavender", R.color.accent_lavender, R.color.accent_lavender_night, R.style.ThemeOverlay_YarnSpace_Accent_Lavender),
    YELLOW("yellow", R.color.accent_yellow, R.color.accent_yellow_night, R.style.ThemeOverlay_YarnSpace_Accent_Yellow),
    PINK("pink", R.color.accent_pink, R.color.accent_pink_night, R.style.ThemeOverlay_YarnSpace_Accent_Pink),
    BLUE("blue", R.color.accent_blue, R.color.accent_blue_night, R.style.ThemeOverlay_YarnSpace_Accent_Blue);

    companion object {
        fun fromBackendName(name: String?): AccentColor {
            return values().find { it.backendName == name } ?: SAGE
        }
    }
}
