package com.yarnspace.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ProfilePublicDto(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("accentColor") val accentColor: String,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,

    @SerializedName("followersCount") val followersCount: Int,
    @SerializedName("followingCount") val followingCount: Int,
    @SerializedName("postsCount") val postsCount: Int,
    @SerializedName("projectsCount") val projectsCount: Int,
    @SerializedName("savedProjectsCount") val savedProjectsCount: Int,

    @SerializedName("isFollowedByMe") val isFollowedByMe: Boolean? = null,
)

data class PostReadDto(
    @SerializedName("id") val id: Int,
    @SerializedName("authorId") val authorId: Int,
    @SerializedName("author") val author: UserPublicDto,
    @SerializedName("content") val content: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("rebloggedProject") val rebloggedProject: ProjectReadDto? = null,
    @SerializedName("createdAt") val createdAt: Long,
)

data class ProjectReadDto(
    @SerializedName("id") val id: Int,
    @SerializedName("authorId") val authorId: Int,
    @SerializedName("author") val author: UserPublicDto,
    @SerializedName("title") val title: String,
    @SerializedName("imageUrl") val imageUrl: String,

    @SerializedName("content") val content: String? = null,
    @SerializedName("hookSize") val hookSize: String? = null,
    @SerializedName("pattern") val pattern: String? = null,
    @SerializedName("yarnType") val yarnType: String? = null,
    @SerializedName("yarnAmount") val yarnAmount: String? = null,
    @SerializedName("timeToComplete") val timeToComplete: String? = null,
    @SerializedName("additionalMaterials") val additionalMaterials: String? = null,

    @SerializedName("isSavedByMe") val isSavedByMe: Boolean? = null,
    @SerializedName("isRebloggedByMe") val isRebloggedByMe: Boolean? = null,
    @SerializedName("createdAt") val createdAt: Long,
)
