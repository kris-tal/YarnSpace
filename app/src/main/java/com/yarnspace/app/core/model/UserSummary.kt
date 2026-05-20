package com.yarnspace.app.core.model

data class UserSummary(
	val id: Long,
	val username: String,
	val displayName: String,
	val avatarUrl: String? = null,
	val avatarResId: Int? = null,
	val accentColor: String? = null,
)


