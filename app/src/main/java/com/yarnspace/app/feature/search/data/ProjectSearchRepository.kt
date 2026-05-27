package com.yarnspace.app.feature.search.data

import com.yarnspace.app.core.model.FeedItem

interface ProjectSearchRepository {
    suspend fun searchProjects(
        query: String,
        hasPatternOnly: Boolean = false,
        createdAfterMillis: Long? = null,
        createdBeforeMillis: Long? = null,
    ): List<FeedItem.Project>
}

