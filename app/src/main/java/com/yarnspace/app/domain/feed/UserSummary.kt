package com.yarnspace.app.domain.feed

data class UserSummary(
    val id: Long,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val avatarResId: Int? = null,
)
