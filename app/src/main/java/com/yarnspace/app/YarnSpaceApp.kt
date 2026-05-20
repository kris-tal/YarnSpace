package com.yarnspace.app

import android.app.Application
import com.yarnspace.app.theme.ThemeModeCoordinator
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class YarnSpaceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeModeCoordinator.applySavedNightMode(this)
    }
}

