package com.yarnspace.app.theme

import androidx.annotation.DrawableRes
import com.yarnspace.app.R

enum class AvatarIcon(val backendName: String, @DrawableRes val resId: Int) {
    DEFAULT("default", R.drawable.ic_avatar_default),

    CAT("=cat", R.drawable.ic_avatar_cat),
    CIRCLE_STAR("circle_star", R.drawable.ic_avatar_circle_star),
    DAISY("daisy", R.drawable.ic_avatar_daisy),
    DAISY_ALT("daisy_alt", R.drawable.ic_avatar_daisy_alt),
    PAW("paw", R.drawable.ic_avatar_paw),
    SMILE("smile", R.drawable.ic_avatar_smile),
    STAR("star", R.drawable.ic_avatar_star);


    companion object {
        fun fromBackendName(name: String?): AvatarIcon {
            return entries.find { it.backendName == name?.lowercase() } ?: DEFAULT
        }
    }
}