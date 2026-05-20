package com.yarnspace.app.feature.search.data

import com.yarnspace.app.core.model.UserSummary

interface UserRepository {
    suspend fun searchUsers(query: String): List<UserSummary>
}

