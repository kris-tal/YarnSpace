package com.yarnspace.app.main
import com.google.android.material.bottomnavigation.BottomNavigationView
class NavigationController(
    private val bottomNavigationView: BottomNavigationView,
    private val onBeforeNavigate: () -> Unit,
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
            onBeforeNavigate()
            onNavigate(item.itemId)
            true
        }
    }
}
