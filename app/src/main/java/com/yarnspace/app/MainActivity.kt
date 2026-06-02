package com.yarnspace.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yarnspace.app.feature.add.presentation.AddFragment
import com.yarnspace.app.feature.feed.presentation.FeedFragment
import com.yarnspace.app.feature.notifs.presentation.NotifsFragment
import com.yarnspace.app.feature.profile.presentation.ProfileFragment
import com.yarnspace.app.feature.search.presentation.SearchFragment
import com.yarnspace.app.main.NavigationController
import com.yarnspace.app.core.theme.AccentThemeCoordinator
import com.yarnspace.app.core.theme.ThemeModeCoordinator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        private const val STATE_SELECTED_NAV_ITEM = "state_selected_nav_item"
    }

    private lateinit var navigationController: NavigationController

    @Inject
    lateinit var themeModeCoordinator: ThemeModeCoordinator
    @Inject
    lateinit var accentThemeCoordinator: AccentThemeCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        themeModeCoordinator.applySavedNightMode()
        accentThemeCoordinator.applySavedAccent(this)

        setContentView(R.layout.activity_main)

        val selectedItemId = savedInstanceState?.getInt(STATE_SELECTED_NAV_ITEM) ?: R.id.nav_feed
        val restoreNavigationState = savedInstanceState != null
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        navigationController = NavigationController(
            bottomNavigationView = bottomNavigationView,
            onNavigate = ::handleBottomNavigation,
        )
        navigationController.bind(
            defaultItemId = selectedItemId,
            restoreState = restoreNavigationState,
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
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

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, fragment)
            .commit()
    }
}