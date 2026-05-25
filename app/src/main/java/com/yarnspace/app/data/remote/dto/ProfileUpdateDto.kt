package com.yarnspace.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ProfileUpdateDto(
    @SerializedName("displayName") val displayName: String,
    @SerializedName("accentColor") val accentColor: String,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
)

data class AvatarUploadResponseDto(
    @SerializedName("avatarUrl") val avatarUrl: String,
)

