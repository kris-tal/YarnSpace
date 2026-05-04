package com.yarnspace.app.data

import com.google.gson.annotations.SerializedName
import com.yarnspace.app.data.remote.dto.UserPublicDto

data class AuthResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("tokenType") val tokenType: String,
    @SerializedName("user") val user: UserPublicDto
)
