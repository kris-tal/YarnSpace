package com.yarnspace.app.theme

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.ContextCompat
import com.yarnspace.app.core.model.UserSummary

fun UserSummary.resolveAccentColorInt(context: Context): Int {
    val isNight = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val accent = AccentColor.fromBackendName(accentColor)
    val resId = if (isNight) accent.nightColorResId else accent.colorResId
    return ContextCompat.getColor(context, resId)
}

