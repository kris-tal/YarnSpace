package com.yarnspace.app.data

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("tokenType") val tokenType: String,
    @SerializedName("user") val user: User
)

data class User(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("nick") val nick: String,
    @SerializedName("accentColor") val accentColor: String,
    @SerializedName("avatarUrl") val avatarUrl: String? = null
)
