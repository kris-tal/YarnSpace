package com.yarnspace.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.yarnspace.app.core.auth.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val next = if (!tokenManager.getToken().isNullOrBlank()) {
            MainActivity::class.java
        } else {
            AuthActivity::class.java
        }

        startActivity(Intent(this, next))
        finish()
    }
}