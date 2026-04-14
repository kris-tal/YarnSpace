package com.yarnspace.app.domain.feed

data class UserSummary(
    val id: Long,
    val username: String,
    val displayName: String,
    val avatarResId: Int? = null, //to potem bedzi url
)

