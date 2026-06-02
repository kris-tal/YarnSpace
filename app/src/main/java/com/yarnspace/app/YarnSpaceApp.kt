package com.yarnspace.app

import android.app.Application
import com.yarnspace.app.core.theme.ThemeModeCoordinator
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class YarnSpaceApp : Application() {

    @Inject
    lateinit var themeModeCoordinator: ThemeModeCoordinator

    override fun onCreate() {
        super.onCreate()
        themeModeCoordinator.applySavedNightMode()
    }
}
