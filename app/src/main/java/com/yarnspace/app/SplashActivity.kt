package com.yarnspace.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.yarnspace.app.core.auth.TokenManager

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val next = if (!TokenManager.getToken(this).isNullOrBlank()) {
            MainActivity::class.java
        } else {
            AuthActivity::class.java
        }

        startActivity(Intent(this, next))
        finish()
    }
}