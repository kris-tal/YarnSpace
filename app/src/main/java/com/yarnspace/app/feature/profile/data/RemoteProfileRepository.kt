package com.yarnspace.app.feature.profile.data

import com.yarnspace.app.core.model.FeedItem
import com.yarnspace.app.core.model.UserSummary
import com.yarnspace.app.core.network.ApiService
import com.yarnspace.app.data.remote.dto.ProfileUpdateDto
import com.yarnspace.app.data.remote.dto.PostReadDto
import com.yarnspace.app.data.remote.dto.ProfilePublicDto
import com.yarnspace.app.data.remote.dto.ProjectReadDto
import com.yarnspace.app.data.remote.dto.UserPrivateDto
import com.yarnspace.app.data.remote.dto.UserPublicDto
import okhttp3.MultipartBody
import javax.inject.Inject

class RemoteProfileRepository @Inject constructor(
    private val apiService: ApiService,
) : ProfileRepository {

    override suspend fun getMeOrNull(): UserPrivateDto? {
        return try {
            apiService.getMe()
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getProfile(username: String): ProfilePublicDto {
        return apiService.getPublicProfile(username)
    }

    override suspend fun listUserPosts(username: String): List<FeedItem> {
        return apiService.listUserPosts(username).map { it.toFeedItemPost() }
    }

    override suspend fun listUserProjects(username: String): List<FeedItem> {
        return apiService.listUserProjects(username).map { it.toFeedItemProject() }
    }

    override suspend fun listMySavedProjects(): List<FeedItem> {
        return apiService.listMySavedProjects().map { it.toFeedItemProject() }
    }

    override suspend fun followUser(username: String) {
        apiService.followUser(username)
    }

    override suspend fun unfollowUser(username: String) {
        apiService.unfollowUser(username)
    }

    override suspend fun updateMe(payload: ProfileUpdateDto): UserPrivateDto {
        return apiService.updateMe(payload)
    }

    override suspend fun uploadMyAvatar(file: MultipartBody.Part): String {
        return apiService.uploadMyAvatar(file).avatarUrl
    }

    private fun UserPublicDto.toUserSummary(): UserSummary {
        return UserSummary(
            id = id.toLong(),
            username = username,
            displayName = displayName,
            avatarUrl = avatarUrl,
            accentColor = accentColor,
        )
    }

    private fun PostReadDto.toFeedItemPost(): FeedItem.Post {
        return FeedItem.Post(
            id = id.toLong(),
            author = author.toUserSummary(),
            createdAt = createdAt,
            content = content,
            imageUrl = imageUrl,
            rebloggedProject = rebloggedProject?.toFeedItemProject(),
        )
    }

    private fun ProjectReadDto.toFeedItemProject(): FeedItem.Project {
        return FeedItem.Project(
            id = id.toLong(),
            author = author.toUserSummary(),
            createdAt = createdAt,
            title = title,
            content = content,
            imageUrl = imageUrl,
            hookSize = hookSize,
            pattern = pattern,
            yarnType = yarnType,
            yarnAmount = yarnAmount,
            timeToComplete = timeToComplete,
            additionalMaterials = additionalMaterials,
            isSavedByMe = isSavedByMe ?: false,
            isRebloggedByMe = isRebloggedByMe ?: false,
        )
    }
}

