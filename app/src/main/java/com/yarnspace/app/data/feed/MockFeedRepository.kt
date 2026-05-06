package com.yarnspace.app.data.feed

import com.yarnspace.app.domain.feed.FeedItem
import com.yarnspace.app.domain.feed.UserSummary

class MockFeedRepository : FeedRepository {

    override suspend fun getFeedItems(): List<FeedItem> {
        val now = System.currentTimeMillis()
        val user1 = UserSummary(
            id = 1,
            username = "kocicaszydelkowania56",
            displayName = "Beata",
            avatarResId = android.R.drawable.ic_menu_gallery
        )
        val user2 = UserSummary(
            id = 2,
            username = "wloczkowakrolowa",
            displayName = "Kasia",
            avatarResId = android.R.drawable.ic_menu_camera
        )
        val user3 = UserSummary(
            id = 3,
            username = "jola",
            displayName = "Jola",
            avatarResId = android.R.drawable.ic_menu_edit
        )

        val project1 = FeedItem.Project(
            id = 100,
            author = user2,
            createdAt = now - 60 * 60 * 1000,
            title = "Szal z gradientem",
            content = "Wzór: prosty ścieg, ale kolory robią robotę.\nZajęło mi 3 wieczory.",
            imageResId = android.R.drawable.ic_menu_gallery,
            hookSize = "4.0 mm",
            yarnType = "merino",
            isSavedByMe = false,
            isRebloggedByMe = false
        )

        val items: List<FeedItem> = listOf(
            project1,
            FeedItem.Post(
                id = 200,
                author = user1,
                createdAt = now - 2 * 60 * 60 * 1000,
                content = "Nie mam siły tego kończyć. Kochani :^( Dość!",
                imageResId = null,
            ),
            FeedItem.Project(
                id = 101,
                author = user1,
                createdAt = now - 5 * 60 * 60 * 1000,
                title = "Pluszowy królik",
                content = "Pierwszy raz robiłam z chenille — nigdy więcej, ale efekt super.",
                imageResId = android.R.drawable.ic_menu_gallery,
                timeToComplete = "2 dni",
                yarnAmount = "~200 g",
                isSavedByMe = true,
                isRebloggedByMe = false
            ),
            FeedItem.Post(
                id = 201,
                author = user3,
                createdAt = now - 8 * 60 * 60 * 1000,
                content = "Fajny ten projekt Kasia!",
                imageResId = null,
                rebloggedProject = project1
            ),
        )

        return items.sortedByDescending { (it as FeedItem.Base).createdAt }
    }

    override suspend fun reblogProject(projectId: Long): Result<FeedItem.Post> {
        return Result.failure(Exception("Mock reblog not implemented"))
    }

    override suspend fun saveProject(projectId: Long): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun unsaveProject(projectId: Long): Result<Unit> {
        return Result.success(Unit)
    }
}
