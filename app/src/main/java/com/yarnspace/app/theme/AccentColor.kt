package com.yarnspace.app.theme

import androidx.annotation.ColorRes
import com.yarnspace.app.R

enum class AccentColor(
    val backendName: String,
    @param:ColorRes val colorResId: Int
) {
    SAGE("sage", R.color.accent_sage),
    PEACH("peach", R.color.accent_peach),
    LAVENDER("lavender", R.color.accent_lavender),
    YELLOW("yellow", R.color.accent_yellow),
    PINK("pink", R.color.accent_pink),
    BLUE("blue", R.color.accent_blue);

    companion object {
        fun fromBackendName(name: String?): AccentColor {
            return values().find { it.backendName == name } ?: SAGE
        }
    }
}
