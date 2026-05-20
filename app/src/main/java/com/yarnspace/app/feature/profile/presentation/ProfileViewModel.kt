package com.yarnspace.app.feature.profile.presentation

import androidx.lifecycle.ViewModel
import com.yarnspace.app.core.model.FeedItem
import com.yarnspace.app.data.remote.dto.ProfilePublicDto

class ProfileViewModel : ViewModel() {
    var profile: ProfilePublicDto? = null
    var posts: List<FeedItem> = emptyList()
    var projects: List<FeedItem> = emptyList()
    var saved: List<FeedItem> = emptyList()

    var viewingUsername: String? = null
    var isFollowedByMe: Boolean = false

    val loadedTabs = mutableSetOf<ProfileTab>()

    enum class ProfileTab { POSTS, PROJECTS, SAVED }

    fun clearCache() {
        profile = null
        posts = emptyList()
        projects = emptyList()
        saved = emptyList()
        loadedTabs.clear()
    }
}

