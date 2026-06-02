package com.yarnspace.app.core.theme

import androidx.annotation.ColorRes
import androidx.annotation.StyleRes
import com.yarnspace.app.R

enum class AccentColor(
    val backendName: String,
    @param:ColorRes val colorResId: Int,
    @param:ColorRes val nightColorResId: Int,
    @param:ColorRes val lighterColorResId: Int,
    @param:ColorRes val nightLighterColorResId: Int,
    @param:ColorRes val darkerColorResId: Int,
    @param:ColorRes val nightDarkerColorResId: Int,
    @param:StyleRes val themeOverlayResId: Int,
) {
    SAGE(
        "sage",
        R.color.accent_sage, R.color.accent_sage_night,
        R.color.lighter_accent_sage, R.color.lighter_accent_sage_night,
        R.color.darker_accent_sage, R.color.darker_accent_sage_night,
        R.style.ThemeOverlay_YarnSpace_Accent_Sage
    ),
    PEACH(
        "peach",
        R.color.accent_peach, R.color.accent_peach_night,
        R.color.lighter_accent_peach, R.color.lighter_accent_peach_night,
        R.color.darker_accent_peach, R.color.darker_accent_peach_night,
        R.style.ThemeOverlay_YarnSpace_Accent_Peach
    ),
    LAVENDER(
        "lavender",
        R.color.accent_lavender, R.color.accent_lavender_night,
        R.color.lighter_accent_lavender, R.color.lighter_accent_lavender_night,
        R.color.darker_accent_lavender, R.color.darker_accent_lavender_night,
        R.style.ThemeOverlay_YarnSpace_Accent_Lavender
    ),
    YELLOW(
        "yellow",
        R.color.accent_yellow, R.color.accent_yellow_night,
        R.color.lighter_accent_yellow, R.color.lighter_accent_yellow_night,
        R.color.darker_accent_yellow, R.color.darker_accent_yellow_night,
        R.style.ThemeOverlay_YarnSpace_Accent_Yellow
    ),
    PINK(
        "pink",
        R.color.accent_pink, R.color.accent_pink_night,
        R.color.lighter_accent_pink, R.color.lighter_accent_pink_night,
        R.color.darker_accent_pink, R.color.darker_accent_pink_night,
        R.style.ThemeOverlay_YarnSpace_Accent_Pink
    ),
    BLUE(
        "blue",
        R.color.accent_blue, R.color.accent_blue_night,
        R.color.lighter_accent_blue, R.color.lighter_accent_blue_night,
        R.color.darker_accent_blue, R.color.darker_accent_blue_night,
        R.style.ThemeOverlay_YarnSpace_Accent_Blue
    );

    fun getLighterShade(isNightMode: Boolean): Int {
        return if (isNightMode) nightLighterColorResId else lighterColorResId
    }

    fun getDarkerShade(isNightMode: Boolean): Int {
        return if (isNightMode) nightDarkerColorResId else darkerColorResId
    }

    companion object {
        fun fromBackendName(name: String?): AccentColor {
            return entries.find { it.backendName == name?.lowercase() } ?: SAGE
        }
    }
}