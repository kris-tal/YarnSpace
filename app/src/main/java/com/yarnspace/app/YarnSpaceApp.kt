package com.yarnspace.app

import android.app.Application
import com.yarnspace.app.theme.ThemeModeCoordinator

class YarnSpaceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeModeCoordinator.applySavedNightMode(this)
    }
}

