package com.yarnspace.app.feature.profile.data

import com.yarnspace.app.core.model.FeedItem
import com.yarnspace.app.data.remote.dto.ProfilePublicDto
import com.yarnspace.app.data.remote.dto.ProfileUpdateDto
import com.yarnspace.app.data.remote.dto.UserPrivateDto

interface ProfileRepository {
    suspend fun getMeOrNull(): UserPrivateDto?

    suspend fun getProfile(username: String): ProfilePublicDto

    suspend fun listUserPosts(username: String): List<FeedItem>

    suspend fun listUserProjects(username: String): List<FeedItem>

    suspend fun listMySavedProjects(): List<FeedItem>

    suspend fun followUser(username: String)

    suspend fun unfollowUser(username: String)

    suspend fun updateMe(payload: ProfileUpdateDto): UserPrivateDto
}

