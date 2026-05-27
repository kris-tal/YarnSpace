package com.yarnspace.app.feature.search.data

import com.yarnspace.app.core.model.FeedItem
import com.yarnspace.app.core.model.UserSummary
import com.yarnspace.app.core.network.ApiService
import com.yarnspace.app.data.remote.dto.ProjectReadDto
import com.yarnspace.app.data.remote.dto.UserPublicDto
import javax.inject.Inject

class RemoteProjectSearchRepository @Inject constructor(
    private val apiService: ApiService,
) : ProjectSearchRepository {

    override suspend fun searchProjects(query: String): List<FeedItem.Project> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()

        return try {
            apiService.searchProjects(q).map { it.toFeedItemProject() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
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

