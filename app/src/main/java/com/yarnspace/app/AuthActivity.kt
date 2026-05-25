package com.yarnspace.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.yarnspace.app.feature.auth.presentation.LoginFragment
import com.yarnspace.app.theme.AccentThemeCoordinator
import com.yarnspace.app.theme.ThemeModeCoordinator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    @Inject
    lateinit var themeModeCoordinator: ThemeModeCoordinator

    @Inject
    lateinit var accentThemeCoordinator: AccentThemeCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        themeModeCoordinator.applySavedNightMode()
        accentThemeCoordinator.applySavedAccent(this)

        setContentView(R.layout.activity_auth)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.auth_container, LoginFragment())
                .commit()
        }
    }
}