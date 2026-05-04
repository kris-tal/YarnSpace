package com.yarnspace.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UserPrivateDto(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("email") val email: String,
    @SerializedName("accentColor") val accentColor: String,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
)

