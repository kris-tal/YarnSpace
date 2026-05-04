package com.yarnspace.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yarnspace.app.data.session.SessionRepository
import com.yarnspace.app.data.settings.ThemeSettingsRepository
import com.yarnspace.app.main.NavigationController
import com.yarnspace.app.main.SettingsPanelController
import com.yarnspace.app.main.SettingsPanelRefs
import com.yarnspace.app.theme.ThemeModeCoordinator

class MainActivity : AppCompatActivity() {

    companion object {
        private const val STATE_SETTINGS_OPEN = "state_settings_open"
        private const val STATE_SELECTED_NAV_ITEM = "state_selected_nav_item"
    }

    private lateinit var settingsController: SettingsPanelController
    private lateinit var navigationController: NavigationController
    private val themeSettingsRepository by lazy { ThemeSettingsRepository(this) }
    private val sessionRepository by lazy { SessionRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeModeCoordinator.applySavedNightMode(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val isSettingsOpen = savedInstanceState?.getBoolean(STATE_SETTINGS_OPEN, false) ?: false
        val selectedItemId = savedInstanceState?.getInt(STATE_SELECTED_NAV_ITEM) ?: R.id.nav_feed
        val restoreNavigationState = savedInstanceState != null
        val settingsRefs = SettingsPanelRefs.from(this)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        settingsController = SettingsPanelController(
            context = this,
            refs = settingsRefs,
            themeSettingsRepository = themeSettingsRepository,
            sessionRepository = sessionRepository,
            resources = resources,
            onLogout = ::logoutToAuth,
        )
        settingsController.bind(isSettingsOpen)

        navigationController = NavigationController(
            bottomNavigationView = bottomNavigationView,
            onBeforeNavigate = { settingsController.closePanelIfOpen() },
            onNavigate = ::handleBottomNavigation,
        )
        navigationController.bind(
            defaultItemId = selectedItemId,
            restoreState = restoreNavigationState,
        )
    }

    override fun onResume() {
        super.onResume()
        settingsController.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_SETTINGS_OPEN, settingsController.isPanelOpen())
        outState.putInt(STATE_SELECTED_NAV_ITEM, findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId)
    }

    private fun handleBottomNavigation(itemId: Int) {
        when (itemId) {
            R.id.nav_search -> loadFragment(SearchFragment())
            R.id.nav_add -> loadFragment(AddFragment())
            R.id.nav_feed -> loadFragment(FeedFragment())
            R.id.nav_profile -> loadFragment(ProfileFragment())
            R.id.nav_notifs -> loadFragment(NotifsFragment())
        }
    }

    private fun logoutToAuth() {
        val intent = Intent(this, AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, fragment)
            .commit()
    }
}
