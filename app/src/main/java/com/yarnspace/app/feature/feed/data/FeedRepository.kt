package com.yarnspace.app.feature.feed.data

import com.yarnspace.app.core.model.FeedItem

interface FeedRepository {
    suspend fun getFeedItems(): List<FeedItem>
    suspend fun deletePost(postId: Long): Result<Unit>
    suspend fun deleteProject(projectId: Long): Result<Unit>
    suspend fun reblogProject(projectId: Long): Result<FeedItem.Post>
    suspend fun saveProject(projectId: Long): Result<Unit>
    suspend fun unsaveProject(projectId: Long): Result<Unit>
}

