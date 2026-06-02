package com.yarnspace.app.core.model

sealed interface FeedItem {

	interface Base : FeedItem {
		val id: Long
		val author: UserSummary
		val createdAt: Long

		val imageResId: Int?
		val imageUrl: String?
	}

	data class Post(
		override val id: Long,
		override val author: UserSummary,
		override val createdAt: Long,
		val content: String?,
		override val imageResId: Int? = null,
		override val imageUrl: String? = null,
		val rebloggedProject: Project? = null,
	) : Base

	data class Project(
		override val id: Long,
		override val author: UserSummary,
		override val createdAt: Long,

		val title: String,
		val content: String? = null,
		override val imageResId: Int? = null,
		override val imageUrl: String? = null,

		val hookSize: String? = null,
		val pattern: String? = null,
		val yarnType: String? = null,
		val yarnAmount: String? = null,
		val timeToComplete: String? = null,
		val additionalMaterials: String? = null,
		val isSavedByMe: Boolean = false,
		val isRebloggedByMe: Boolean = false,
	) : Base
}