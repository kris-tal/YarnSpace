package com.yarnspace.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PostCreateDto(
    @SerializedName("content") val content: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("rebloggedProjectId") val rebloggedProjectId: Long? = null,
)

data class ProjectCreateDto(
    @SerializedName("title") val title: String,
    @SerializedName("imageUrl") val imageUrl: String,

    @SerializedName("content") val content: String? = null,
    @SerializedName("hookSize") val hookSize: String? = null,
    @SerializedName("pattern") val pattern: String? = null,
    @SerializedName("yarnType") val yarnType: String? = null,
    @SerializedName("yarnAmount") val yarnAmount: String? = null,
    @SerializedName("timeToComplete") val timeToComplete: String? = null,
    @SerializedName("additionalMaterials") val additionalMaterials: String? = null,
)
