package com.yarnspace.app.data.profile

import com.yarnspace.app.domain.feed.FeedItem
import com.yarnspace.app.domain.feed.UserSummary

object MockProfileRepository {

    fun getMyUser(): UserSummary = UserSummary(
        id = 1L,
        username = "kocicaszydelkowania56",
        displayName = "Beata",
        avatarResId = android.R.drawable.ic_menu_gallery
    )

    fun getMyPosts(now: Long = System.currentTimeMillis()): List<FeedItem> {
        val me = getMyUser()
        val items: List<FeedItem> = listOf(
            FeedItem.Post(
                id = 501,
                author = me,
                createdAt = now - 30 * 60 * 1000,
                content = "Kawa, włóczka i cisza. Idealny wieczór.",
                imageResId = null,
            ),
            FeedItem.Post(
                id = 502,
                author = me,
                createdAt = now - 3 * 60 * 60 * 1000,
                content = "Udało mi się ogarnąć brzegi bez przeklinania. Sukces!",
                imageResId = null,
            ),
            FeedItem.Post(
                id = 503,
                author = me,
                createdAt = now - 26 * 60 * 60 * 1000,
                content = "Mały update: robię próbkę ściegu przed dużym projektem. (Tak, nauczyłam się.)",
                imageResId = null,
            ),
            FeedItem.Project(
                id = 101,
                author = me,
                createdAt = now - 15 * 60 * 60 * 1000,
                title = "Pluszowy królik",
                content = "Pierwszy raz robiłam z z nici — nigdy więcej, ale efekt super.",
                imageResId = android.R.drawable.ic_menu_gallery,
                timeToComplete = "2 dni",
                yarnAmount = "~200 g",
            ),
            FeedItem.Post(
                id = 504,
                author = me,
                createdAt = now - 10 * 60 * 60 * 1000,
                content = "Ugh.",
                imageResId = null,
            ),
        )

        return items.sortedByDescending { (it as FeedItem.Base).createdAt }
    }
}
