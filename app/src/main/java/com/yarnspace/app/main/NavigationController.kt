package com.yarnspace.app.main
import com.google.android.material.bottomnavigation.BottomNavigationView
class NavigationController(
    private val bottomNavigationView: BottomNavigationView,
    private val onNavigate: (Int) -> Unit,
) {
    fun bind(defaultItemId: Int, restoreState: Boolean = false) {
        if (bottomNavigationView.selectedItemId != defaultItemId) {
            bottomNavigationView.selectedItemId = defaultItemId
        }
        if (!restoreState) {
            onNavigate(defaultItemId)
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            onNavigate(item.itemId)
            true
        }
    }
}
