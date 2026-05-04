package com.yarnspace.app.data.user

import com.yarnspace.app.domain.feed.UserSummary

interface UserRepository {
    suspend fun searchUsers(query: String): List<UserSummary>
}
