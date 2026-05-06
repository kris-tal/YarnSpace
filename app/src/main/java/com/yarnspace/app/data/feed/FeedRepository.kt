package com.yarnspace.app.data.feed

import com.yarnspace.app.domain.feed.FeedItem

interface FeedRepository {
    suspend fun getFeedItems(): List<FeedItem>
    suspend fun reblogProject(projectId: Long): Result<FeedItem.Post>
    suspend fun saveProject(projectId: Long): Result<Unit>
    suspend fun unsaveProject(projectId: Long): Result<Unit>
}
