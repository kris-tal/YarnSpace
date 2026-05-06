package com.yarnspace.app.theme

import androidx.annotation.ColorRes
import com.yarnspace.app.R

enum class AccentColor(
    val backendName: String,
    @param:ColorRes val colorResId: Int,
    @param:ColorRes val nightColorResId: Int,
) {
    SAGE("sage", R.color.accent_sage, R.color.accent_sage_night),
    PEACH("peach", R.color.accent_peach, R.color.accent_peach_night),
    LAVENDER("lavender", R.color.accent_lavender, R.color.accent_lavender_night),
    YELLOW("yellow", R.color.accent_yellow, R.color.accent_yellow_night),
    PINK("pink", R.color.accent_pink, R.color.accent_pink_night),
    BLUE("blue", R.color.accent_blue, R.color.accent_blue_night);

    companion object {
        fun fromBackendName(name: String?): AccentColor {
            return values().find { it.backendName == name } ?: SAGE
        }
    }
}
