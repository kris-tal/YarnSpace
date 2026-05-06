package com.yarnspace.app.data.feed

import android.content.Context
import com.google.gson.Gson
import com.yarnspace.app.data.remote.dto.PostCreateDto
import com.yarnspace.app.data.remote.dto.PostReadDto
import com.yarnspace.app.data.remote.dto.ProjectReadDto
import com.yarnspace.app.data.remote.dto.UserPublicDto
import com.yarnspace.app.domain.feed.FeedItem
import com.yarnspace.app.domain.feed.UserSummary
import com.yarnspace.app.retrofit.RetrofitClient

class RemoteFeedRepository(private val context: Context) : FeedRepository {

    private val gson = Gson()

    override suspend fun getFeedItems(): List<FeedItem> {
        return try {
            val api = RetrofitClient.getInstance(context)
            val jsonElements = api.getGlobalFeed()
            
            jsonElements.mapNotNull { element ->
                val obj = element.asJsonObject
                if (obj.has("title")) {
                    //project
                    val dto = gson.fromJson(element, ProjectReadDto::class.java)
                    dto.toFeedItemProject()
                } else {
                    //post
                    val dto = gson.fromJson(element, PostReadDto::class.java)
                    dto.toFeedItemPost()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun reblogProject(projectId: Long): Result<FeedItem.Post> {
        return try {
            val api = RetrofitClient.getInstance(context)
            val response = api.createPost(PostCreateDto(rebloggedProjectId = projectId))
            Result.success(response.toFeedItemPost())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveProject(projectId: Long): Result<Unit> {
        return try {
            RetrofitClient.getInstance(context).saveProject(projectId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unsaveProject(projectId: Long): Result<Unit> {
        return try {
            RetrofitClient.getInstance(context).unsaveProject(projectId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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

    private fun PostReadDto.toFeedItemPost(): FeedItem.Post {
        return FeedItem.Post(
            id = id.toLong(),
            author = author.toUserSummary(),
            createdAt = createdAt,
            content = content,
            imageUrl = imageUrl,
            rebloggedProject = rebloggedProject?.toFeedItemProject()
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
            isRebloggedByMe = isRebloggedByMe ?: false
        )
    }
}
